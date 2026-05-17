"""Crawler FO4DB → POST /api/v1/admin/sync/cards (PlayerCardDTO).

Vị trí chỉ ghi trên thẻ ``fco_player_cards.preferred_position``.
Bảng ``fco_players`` không còn ``primary_position`` — script không gửi/sync trường đó.
"""

import json
import re
import time
from datetime import datetime, timezone
from typing import Any, Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

BASE_URL = "https://vn.fifaaddict.com/fo4db"
SYNC_API_URL = "http://localhost:8080/api/v1/admin/sync/cards"
HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    )
}

# OVR trên site là “gốc”; backend/game logic của bạn lưu OVR = site + bonus (tối đa 255).
OVR_BONUS_BEFORE_SYNC = 4
OVR_MAX = 255

def get_admin_token() -> str:
    login_url = "http://localhost:8080/api/v1/auth/login"
    
    # THAY BẰNG TÀI KHOẢN ADMIN TRONG DATABASE CỦA BẠN
    payload = {
        "identifier": "admin", 
        "password": "admin12345" 
    }
    
    print("[auth] Đang đăng nhập lấy Token Admin...")
    try:
        resp = requests.post(login_url, json=payload, timeout=10)
        resp.raise_for_status()
        
        # Trích xuất Token từ Response Cookie (do backend đã đổi sang HttpOnly cookie)
        token = resp.cookies.get("access_token")
        if not token:
            data = resp.json().get("data", {})
            token = data.get("accessToken") or data.get("token")
            
        if not token:
            raise ValueError("Đăng nhập thành công nhưng không tìm thấy accessToken trong Cookie hoặc JSON.")
            
        print("[auth] Lấy Token thành công!")
        return token
    except Exception as e:
        print(f"❌ Lỗi đăng nhập: {e}")
        exit(1) # Dừng luôn script nếu không lấy được token

def create_session() -> requests.Session:
    retry = Retry(
        total=5,
        connect=3,
        read=3,
        backoff_factor=1.4,
        status_forcelist=(429, 500, 502, 503, 504),
        allowed_methods=("GET",),
    )
    adapter = HTTPAdapter(max_retries=retry)
    session = requests.Session()
    session.headers.update(HEADERS)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session


def to_int(value: Optional[str]) -> Optional[int]:
    try:
        if not value:
            return None
        digits = re.sub(r"[^0-9]", "", value)
        if not digits:
            return None
        return int(digits)
    except Exception:
        return None


def extract_external_id(player_url: str, row: Any) -> Optional[str]:
    try:
        data_external = row.get("data-playerid") or row.get("data-id")
        if data_external:
            return str(data_external).strip()
    except Exception:
        pass

    try:
        normalized = player_url.split("?")[0].rstrip("/")
        external_id = normalized.split("/")[-1]
        return external_id.strip() if external_id else None
    except Exception:
        return None


def extract_enhance_level(row: Any) -> int:
    # Mặc định cấp 1 nếu không đọc được từ DOM.
    default_level = 1

    try:
        data_level = row.get("data-level") or row.get("data-enhance-level")
        parsed = to_int(str(data_level)) if data_level is not None else None
        if parsed and parsed > 0:
            return parsed
    except Exception:
        pass

    try:
        text = row.get_text(" ", strip=True)
        # Bắt các pattern như +5, lv.5, level 5...
        for pattern in (r"\+(\d{1,2})", r"lv\.?\s*(\d{1,2})", r"level\s*(\d{1,2})"):
            match = re.search(pattern, text, flags=re.IGNORECASE)
            if match:
                parsed = to_int(match.group(1))
                if parsed and parsed > 0:
                    return parsed
    except Exception:
        pass

    return default_level


def extract_season_code(row: Any) -> str:
    try:
        badge = row.select_one("img.badgedss")
        if not badge:
            return "UNKNOWN"
        for cls in badge.get("class", []):
            if cls and re.match(r"^[a-z0-9]+$", cls):
                return cls.upper()
    except Exception:
        pass
    return "UNKNOWN"

def extract_position(row: Any) -> Optional[str]:
    """Lấy vị trí ưu tiên (POS), ví dụ ST, CAM, CB — trả None nếu không đọc được (DB lưu NULL)."""
    try:
        pos_tag = (
            row.select_one("td.pos")
            or row.select_one(".pos")
            or row.select_one("td[class*='pos']")
            or row.select_one("[data-pos]")
        )
        if pos_tag:
            pos_text = pos_tag.get_text(" ", strip=True)
            if not pos_text and pos_tag.has_attr("data-pos"):
                pos_text = str(pos_tag["data-pos"]).strip()
            upper = pos_text.upper()
            # Ưu tiên mã 2–4 chữ cái đầu tiên (tránh ghép "ST / CF" thành "STCF")
            m = re.search(r"\b([A-Z]{2,4})\b", upper)
            if m:
                return m.group(1)
            pos_clean = re.sub(r"[^A-Z]", "", upper)
            if pos_clean:
                return pos_clean[:10]
    except Exception:
        pass
    return None


def extract_salary(row: Any) -> int:
    """Lấy chỉ số lương FP trên thẻ (số nguyên). Không parse được thì 0 (hợp lệ với backend @PositiveOrZero)."""
    selectors = (
        "td.fp",
        "td.pay",
        "td.wage",
        "td.salary",
        ".fp",
        ".pay",
        "[data-fp]",
        "[data-salary]",
    )
    try:
        for sel in selectors:
            fp_tag = row.select_one(sel)
            if not fp_tag:
                continue
            text = fp_tag.get_text(" ", strip=True)
            if not text and fp_tag.attrs:
                for key in ("data-fp", "data-salary", "data-pay"):
                    if fp_tag.has_attr(key):
                        text = str(fp_tag[key]).strip()
                        break
            parsed = to_int(text)
            if parsed is not None:
                return max(0, parsed)
    except Exception:
        pass
    return 0

def parse_player_row(row: Any, crawl_at: str) -> Optional[dict[str, Any]]:
    try:
        info_cell = row.select_one("td.info")
        if not info_cell:
            return None
    except Exception:
        return None

    try:
        name_tag = info_cell.select_one("a.player-name")
        if not name_tag:
            name_tag = info_cell.select_one(".name")
            
        if not name_tag:
            return None
            
        player_url = urljoin("https://vn.fifaaddict.com", name_tag.get("href", "")) if name_tag.name == 'a' else ""
        player_name = name_tag.get_text(" ", strip=True)
    except Exception:
        return None

    try:
        raw_ovr = to_int(
            row.select_one("td.ovr").get_text(" ", strip=True) if row.select_one("td.ovr") else None
        )
        if raw_ovr is None:
            return None
        ovr = min(max(raw_ovr + OVR_BONUS_BEFORE_SYNC, 0), OVR_MAX)
    except Exception:
        return None

    try:
        market_price_text = row.select_one("td.pricekrhide").get_text(" ", strip=True) if row.select_one("td.pricekrhide") else None
    except Exception:
        market_price_text = None

    try:
        image_tag = info_cell.select_one("img.thumb")
        image_url = urljoin("https://vn.fifaaddict.com", image_tag.get("src", "")) if image_tag else None
    except Exception:
        image_url = None

    external_id = extract_external_id(player_url, row)
    if not external_id:
        return None

    position = extract_position(row)
    salary = extract_salary(row)

    payload: dict[str, Any] = {
        "external_id": external_id,
        "name": player_name,
        "season_code": extract_season_code(row),
        "enhance_level": extract_enhance_level(row),
        "ovr": ovr,
        "ovr_site": raw_ovr,
        "salary": salary,
        "market_price_bp": to_int(market_price_text),
        "market_price_text": market_price_text,
        "source_url": player_url,
        "image_url": image_url,
        "crawl_at": crawl_at,
    }
    if position:
        payload["preferredPosition"] = position
    return payload

def crawl_season_database(season_codes: list[str]) -> list[dict[str, Any]]:
    session = create_session()
    crawl_at = datetime.now(timezone.utc).isoformat()
    collected: list[dict[str, Any]] = []

    for season in season_codes:
        season_code = (season or "").strip().upper()
        if not season_code:
            continue

        try:
            response = session.get(BASE_URL, params={"sv": "vn", "class": season_code.lower()}, timeout=20)
            response.raise_for_status()
        except Exception as ex:
            print(f"[warn] season={season_code} request failed: {ex}")
            continue

        try:
            soup = BeautifulSoup(response.text, "html.parser")
            rows = soup.select("table tbody tr")
        except Exception as ex:
            print(f"[warn] season={season_code} parse failed: {ex}")
            continue

        if not rows:
            print(f"[info] season={season_code} không có dữ liệu")
            continue

        count = 0
        for row in rows:
            try:
                item = parse_player_row(row, crawl_at)
                if not item:
                    continue
                # Luôn gắn mã mùa theo URL (?class=...) — khớp bảng mùa trên site.
                item["season_code"] = season_code
                collected.append(item)
                count += 1
            except Exception:
                continue

        print(f"[info] season={season_code} collected={count} cầu thủ")
        time.sleep(1.2)

    deduped: dict[tuple[str, str, int], dict[str, Any]] = {}
    for item in collected:
        try:
            key = (item["external_id"], item["season_code"], int(item["enhance_level"]))
            deduped[key] = item
        except Exception:
            continue

    return list(deduped.values())


def sync_to_api(players: list[dict[str, Any]], endpoint: str, token: str, chunk_size: int = 1000) -> None:
    if not players:
        return
        
    session = requests.Session()
    # NHÉT TOKEN VÀO HEADER TẠI ĐÂY
    session.headers.update({
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    })
    
    total = len(players)
    print(f"\n[sync] Bắt đầu đẩy {total} cầu thủ lên API: {endpoint}")
    
    # ... (Giữ nguyên đoạn code vòng lặp for i in range... bên dưới của bạn)
    for i in range(0, total, chunk_size):
        chunk = players[i:i + chunk_size]
        success = False
        
        # Thử lại tối đa 3 lần cho mỗi chunk
        for attempt in range(1, 4):
            try:
                resp = session.post(endpoint, json=chunk, timeout=60)
                if resp.status_code in (200, 201):
                    print(f"[sync] Thành công chunk {i} -> {i + len(chunk)} (HTTP {resp.status_code})")
                    try:
                        body = resp.json()
                        stats = body.get("data") or body
                        tr = stats.get("totalReceived")
                        ts = stats.get("totalSuccessful")
                        tf = stats.get("totalFailed")
                        if tr is not None:
                            print(
                                f"[sync] API báo: totalReceived={tr} totalSuccessful={ts} "
                                f"totalFailed={tf} (nếu successful=0 thì DB gần như không ghi thẻ)"
                            )
                    except Exception:
                        pass
                    success = True
                    break # Thoát vòng lặp retry nếu thành công
                else:
                    print(f"[sync] Lần thử {attempt}: Cảnh báo chunk {i} - HTTP {resp.status_code}: {resp.text}")
            except Exception as exc:
                print(f"[sync] Lần thử {attempt}: Lỗi mạng khi đẩy chunk {i}: {exc}")
            
            time.sleep(2) # Nghỉ 2s trước khi thử lại
            
        if not success:
            print(f"❌ THẤT BẠI TẤT CẢ CÁC LẦN THỬ CHO CHUNK {i} -> {i + len(chunk)}")

if __name__ == "__main__":
    ALL_SEASONS = [
    "icontm", "icontmb", "icon", "wg", "fac", "25dp", "fsl", "ws", "dcb", "le" 
    # "ch", "wb", "no7", "gru", "bld", "bdo", "24ep", "cu", "mdl", "23ty", 
    # "24tyn", "24ty", "25tyn", "25ty", "26tyn", "26ty", "wc22", "bwc", "rtn", "hg", 
    # "cc", "23hw", "fc", "dc", "jnm", "ut", "ld", "eu24", "23tyn", "22ty", 
    # "21ty", "22tyn", "21tyn", "20ty", "20tyn", "19ty", "18ty", "25ts", "24ts", "23ts", 
    # "22ts", "21ts", "20ts", "19ts", "nhd", "tb", "tt", "coc", "lh", "mog", 
    # "vtr", "mc", "up", "ebs", "btb", "cap", "boe21", "boe", "otw", "fa", 
    # "lol", "ln", "spl", "hot", "tc", "gr", "26hr", "25hr", "24hr", "23hr", 
    # "22hr", "23ng", "22ng", "21ng", "20ng", "19ng", "25ucl", "24ucl", "22ucl", "23ucl", 
    # "21ucl", "20ucl", "18", "19ucl", "17", "25pl", "ntg", "24pl", "ja", "20", 
    # "19", "21", "22", "24", "23", "live", "18a", "18s", "19a", "19s", 
    # "20a", "21pl", "22pl", "23pl", "rmfc", "25im", "k23", "cfa", "25imf", "k22", 
    # "la", "iconm", "k21", "psg", "khd", "k19", "mci", "tki", "k18", "mcc", 
    # "tkl", "22tg", "vla", "21tg", "12kh", "25th", "25kl", "24wl", "thb", "24kl", 
    # "23wl", "thl", "23kl", "22wl", "vnl", "22kl", "21wl", "25vb", "21kl", "20wl", 
    # "24vb", "20kl", "19wl", "23vb", "25klb", "syl", "22vb", "24klb", "dyl", "vfg", 
    # "23klb", "ryl", "vn", "22klb", "tyl", "prm", "21klb", "mls", "hc", "20klb", 
    # "hyl3", "tk", "hyl2", "hyl1", "el", "flg", "25csl"
]
    output = crawl_season_database(ALL_SEASONS)

    print(
        f"[info] OVR gửi lên API = OVR trên site + {OVR_BONUS_BEFORE_SYNC} (tối đa {OVR_MAX}); "
        "file JSON có thêm trường ovr_site để đối chiếu."
    )
    
    # 1. Lưu ra file json để backup
    with open("../src/main/resources/data/players.json", "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    print(f"done: {len(output)} records saved locally.")
    
    # 2. LẤY TOKEN ADMIN
    admin_token = get_admin_token()
    
    # 3. Bắn thẳng sang API Backend VỚI TOKEN
    sync_to_api(output, SYNC_API_URL, admin_token)
