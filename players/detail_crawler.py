"""
detail_crawler.py  —  Pha B: Crawl trang chi tiết từng cầu thủ FIFAAddict
===========================================================================
Chức năng:
  - Đọc danh sách external_id từ players.json (output của Pha A)
    Format external_id: "pidzlmgnynqv" (có tiền tố "pid")
  - Crawl trang /fo4db/{external_id} để lấy:
      + 6 chỉ số chính (pace, shooting, passing, dribbling, defending, physicality)
      + LivePerf (0 = không có, > 0 = có — OVR đã bao gồm bonus này rồi)
      + Traits / chỉ số ẩn
      + Giá thị trường 13 grade
      + Thông tin bio cầu thủ (height, weight, birthdate, foot...)
      + Thông tin Nation / Club / League để auto-populate bảng danh mục
      + OVR theo từng vị trí
  - Sync lên API: POST /api/v1/admin/sync/cards/detail
  - Backend ghi lỗi từng bản ghi (exception, skip không có player/card) vào bảng fco_detail_sync_errors
  - Nếu cả chunk POST thất bại (HTTP/mạng): script ghi thêm detail_sync_api_errors.jsonl

Cách chạy:
  python detail_crawler.py               # Chạy toàn bộ
  python detail_crawler.py --limit 5     # Test 5 cầu thủ đầu
  python detail_crawler.py --resume      # Bỏ qua ID đã crawl (detail_crawled.json)
  python detail_crawler.py --no-sync     # Chỉ crawl, không gửi API
"""

import json
import os
import re
import subprocess
import sys
import io
import tempfile
import time
import argparse
from datetime import datetime, timezone
from typing import Any, Optional

import requests
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# ─── Cấu hình ────────────────────────────────────────────────────────────────
if hasattr(sys.stdout, 'buffer'):
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# external_id từ players.json đã có dạng "pidzlmgnynqv"
# → URL chi tiết: https://vn.fifaaddict.com/fo4db/pidzlmgnynqv
BASE_DETAIL_URL = "https://vn.fifaaddict.com/fo4db/{external_id}"

SYNC_DETAIL_API = "http://localhost:8080/api/v1/admin/sync/cards/detail"
LOGIN_URL       = "http://localhost:8080/api/v1/auth/login"

ADMIN_EMAIL     = "admin"   # ← Thay email admin
ADMIN_PASSWORD  = "admin12345"       # ← Thay password admin

OVR_BONUS         = 4       # Level 5 kinh nghiệm = Level 1 + 4
DELAY_SECONDS     = 1.5     # Delay giữa mỗi request tránh rate-limit
CHUNK_SIZE        = 50      # Số record gửi lên API mỗi lần
CHECKPOINT_EVERY  = 10      # Lưu done list mỗi N cầu thủ (phòng crash)
INPUT_FILE        = "../src/main/resources/data/players.json"
CRAWLED_FILE      = "../src/main/resources/data/detail_crawled.json"    # đã crawl HTML thành công
SYNCED_FILE       = "../src/main/resources/data/detail_synced.json"      # đã POST API chunk thành công
DONE_FILE         = "../src/main/resources/data/detail_done.json"       # summary / report, không dùng làm checkpoint
# Ghi lỗi HTTP / mạng khi POST chunk (bổ sung cho bảng DB fco_detail_sync_errors của backend)
DETAIL_API_ERROR_LOG = "../src/main/resources/data/detail_sync_api_errors.jsonl"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "vi-VN,vi;q=0.9",
}


# ─── Auth ─────────────────────────────────────────────────────────────────────

def get_admin_token() -> str:
    print("[auth] Đang đăng nhập lấy Token Admin...")
    resp = requests.post(
        LOGIN_URL,
        json={"identifier": ADMIN_EMAIL, "password": ADMIN_PASSWORD},
        timeout=10
    )
    resp.raise_for_status()
    # Backend sử dụng HttpOnly Cookie cho access_token
    token = resp.cookies.get("access_token")
    if not token:
        data = resp.json().get("data", {})
        token = data.get("accessToken") or data.get("token")
        if not token:
            raise ValueError("Không tìm thấy accessToken trong response Cookie/JSON")
    print("[auth] Token OK!")
    return token


# ─── HTTP Session ──────────────────────────────────────────────────────────────

def create_session() -> requests.Session:
    retry = Retry(
        total=4, backoff_factor=2,
        status_forcelist=(429, 500, 502, 503, 504),
        allowed_methods=("GET",),
    )
    session = requests.Session()
    session.headers.update(HEADERS)
    session.mount("https://", HTTPAdapter(max_retries=retry))
    return session


# ─── Nuxt State Parser (Node.js) ──────────────────────────────────────────────

NODE_FAIL_COUNT = 0
NODE_FAIL_THRESHOLD = 3
NODE_FAIL_COOLDOWN_SECONDS = 10
NODE_LAST_FAIL_AT = 0.0


def eval_nuxt_via_node(nuxt_script: str) -> dict:
    """
    Dùng Node.js eval JavaScript thật của Nuxt → object data đầy đủ.
    Tránh mọi vấn đề encoding / ký tự đặc biệt khi parse thủ công.
    """
    global NODE_FAIL_COUNT, NODE_LAST_FAIL_AT
    if NODE_FAIL_COUNT >= NODE_FAIL_THRESHOLD:
        elapsed = time.time() - NODE_LAST_FAIL_AT
        if elapsed < NODE_FAIL_COOLDOWN_SECONDS:
            raise RuntimeError(f"Node.js tạm cooldown do lỗi liên tiếp ({NODE_FAIL_COUNT})")
        NODE_FAIL_COUNT = 0

    node_code = f"""
const window = {{}};
{nuxt_script}
function safeStringify(obj) {{
  const seen = new WeakSet();
  return JSON.stringify(obj, (key, value) => {{
    if (typeof value === 'object' && value !== null) {{
      if (seen.has(value)) return undefined;
      seen.add(value);
    }}
    return value;
  }});
}}
try {{
  const data = window.__NUXT__.data[0];
  delete data.foCommentSSR;
  process.stdout.write(safeStringify(data));
}} catch(e) {{
  process.stdout.write(JSON.stringify({{_node_error: e.message}}));
}}
"""
    tmp = tempfile.NamedTemporaryFile(mode='w', suffix='.js', delete=False, encoding='utf-8')
    tmp.write(node_code)
    tmp.close()
    try:
        result = subprocess.run(
            ["node", tmp.name],
            capture_output=True, text=True, timeout=15, encoding='utf-8'
        )
        if result.returncode != 0:
            NODE_FAIL_COUNT += 1
            NODE_LAST_FAIL_AT = time.time()
            raise RuntimeError(f"Node.js stderr: {result.stderr[:300]}")
        NODE_FAIL_COUNT = 0
        return json.loads(result.stdout)
    except Exception:
        NODE_FAIL_COUNT += 1
        NODE_LAST_FAIL_AT = time.time()
        raise
    finally:
        os.unlink(tmp.name)


def extract_nuxt_script(html: str) -> Optional[str]:
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup.find_all("script"):
        if tag.string and "window.__NUXT__" in tag.string:
            return tag.string
    return None


# ─── Build payload ────────────────────────────────────────────────────────────

def atomic_write_json(path: str, data: Any) -> None:
    tmp_path = f"{path}.tmp"
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp_path, path)


def parse_price_raw(price_raw: str) -> Optional[int]:
    """
    Chuyển "33,800B" → 33800 (đơn vị lưu DB = nghìn tỷ BP game).
    FIFAAddict hiển thị số đã là giá trị cuối, không cần nhân thêm.
    """
    try:
        return int(re.sub(r"[^\d]", "", price_raw.replace(",", "")))
    except Exception:
        return None


def validate_payload(payload: dict) -> bool:
    if not isinstance(payload, dict):
        return False
    if not payload.get("external_id"):
        return False
    if not isinstance(payload.get("traits"), list):
        return False
    if not isinstance(payload.get("prices"), list):
        return False
    if not isinstance(payload.get("card_detail"), dict):
        return False
    card_detail = payload["card_detail"]
    for key in ("pace", "shooting", "passing", "dribbling", "defending", "physicality"):
        value = card_detail.get(key)
        if value is not None and (not isinstance(value, int) or value < 0):
            return False
    return True


def build_payload(raw: dict, external_id: str) -> Optional[dict]:
    """
    Chuyển Nuxt raw data → payload gửi lên API backend.

    external_id: "pidzlmgnynqv"
    uid trong nuxt: "zlmgnynqv" (không có "pid")
    """
    db = raw.get("foPlayerSSRdb", {})
    if not db:
        return None

    traits_raw = raw.get("foTraitsSSR", {})
    price_raw  = raw.get("foPriceSSR",  {})
    relate     = raw.get("foPlayerSSRrelate", [])

    # uid trong nuxt KHÔNG có "pid" prefix
    uid = db.get("uid", "")  # VD: "zlmgnynqv"

    # ── LivePerf ─────────────────────────────────────────────────────
    # Chỉ cần kiểm tra thẻ HIỆN TẠI có liveperf không.
    # OVR trên FIFAAddict đã bao gồm bonus liveperf → không cộng thêm.
    liveperf = 0
    for season in relate:
        # So sánh uid (không có "pid") với uid trong relate list
        if season.get("uid") == uid:
            liveperf = season.get("liveperf", 0)
            break

    # ── 6 chỉ số chính ───────────────────────────────────────────────
    attr_group  = db.get("attrgroup", {})
    attr_labels = attr_group.get("labels", [])
    attr_values = attr_group.get("data", [])
    attrs       = dict(zip(attr_labels, attr_values))

    ATTR_MAP = {
        "Tốc độ":   "pace",
        "Sút":       "shooting",
        "Chuyền":    "passing",
        "Rê bóng":   "dribbling",
        "Phòng thủ": "defending",
        "Thể lực":   "physicality",
    }
    six_lv1 = {}
    for vn, eng in ATTR_MAP.items():
        v = attrs.get(vn, 0)
        try:
            six_lv1[eng] = int(v) if v is not None else 0
        except Exception:
            six_lv1[eng] = 0
    six_lv5 = {k: v + OVR_BONUS for k, v in six_lv1.items()}

    # ── OVR theo vị trí ──────────────────────────────────────────────
    postlist   = db.get("postlist", {})
    ovr_by_pos = {v.get("name", k): v.get("value") for k, v in postlist.items()}

    # ── Traits ───────────────────────────────────────────────────────
    traits = [
        {
            "trait_code":   code,
            "trait_name":   t.get("name", ""),
            "description": t.get("desc", ""),
            "icon_id":      t.get("id", ""),
            "icon_url": (
                f"https://s1.fifaaddict.com/fo4/traits/trait_icon_{t.get('id')}.png"
                if t.get("id") else None
            ),
        }
        for code, t in traits_raw.items()
    ]

    # ── Giá 13 grade ─────────────────────────────────────────────────
    price_date = price_raw.get("updatetime")
    prices = [
        {
            "grade":     i,
            "price_raw":  price_raw[f"kr{i}"],
            "price_bp":   parse_price_raw(price_raw[f"kr{i}"]),
            "price_date": price_date,
        }
        for i in range(1, 14)
        if f"kr{i}" in price_raw
    ]

    # ── Thông tin CLB / Nation / League để populate bảng danh mục ───
    club_id_fifaaddict = db.get("team_id")
    club_slug  = db.get("team_slug")
    nation_slug = db.get("nation_slug")
    league_slug = db.get("league_slug")

    clubs = []
    if db.get("team_name"):
        try:
            club_fifa_id = int(club_id_fifaaddict) if club_id_fifaaddict else None
        except Exception:
            club_fifa_id = None
        clubs.append({
            "club_name": db.get("team_name"),
            "club_slug": club_slug,
            "club_fifaaddict_id": club_fifa_id,
            "crest_url": (
                f"https://s1.fifaaddict.com/fo4/crests/dark/d{club_id_fifaaddict}.png"
                if club_id_fifaaddict else None
            )
        })

    return {
        # ── Identifier ──────────────────────────────────────────────
        "external_id": external_id,          # "pidzlmgnynqv" — khớp với DB

        # ── Bio cầu thủ (→ fco_players) ─────────────────────────────
        "player_bio": {
            "height":         db.get("height"),
            "weight":         db.get("weight"),
            "birthdate":      db.get("birthdate"),
            "preferred_foot":  db.get("foot_pref"),
            "weak_foot":       db.get("foot_weak"),
            # Nation
            "nation_name":     db.get("nation_name"),
            "nation_slug":     nation_slug,
            # Club
            "clubs":           clubs,
            # League
            "league_name":     db.get("league_name"),
            "league_slug":     league_slug,
        },

        # ── Thẻ chi tiết (→ fco_player_cards) ──────────────────────
        "card_detail": {
            "liveperf":          liveperf,
            "has_live_perf":       liveperf > 0,
            "skill_level":        db.get("skill_level"),
            "secondary_position": db.get("pos2"),
            "workerate_att":      db.get("workrate_att"),
            "workerate_def":      db.get("workrate_def"),
            "bodytype":          db.get("bodytype_name"),
            "reputation":        db.get("reputation"),
            "price_updated_at":    price_date,
            "ovr_by_pos":           ovr_by_pos,

            # 6 chỉ số Level 5 (giá trị lưu vào DB)
            "pace":        six_lv5["pace"],
            "shooting":    six_lv5["shooting"],
            "passing":     six_lv5["passing"],
            "dribbling":   six_lv5["dribbling"],
            "defending":   six_lv5["defending"],
            "physicality": six_lv5["physicality"],
        },

        # ── Traits (→ fco_traits + fco_player_card_traits) ──────────
        "traits": traits,

        # ── Giá (→ fco_card_prices) ──────────────────────────────────
        "prices": prices,
    }


# ─── Crawl một cầu thủ ────────────────────────────────────────────────────────

def crawl_one(session: requests.Session, external_id: str) -> Optional[dict]:
    """
    Crawl trang chi tiết của 1 cầu thủ và trả về payload dict.
    Được gọi lặp lại trong vòng lặp main() cho từng external_id.

    Ví dụ: external_id = "pidzlmgnynqv"
           → GET https://vn.fifaaddict.com/fo4db/pidzlmgnynqv
    """
    url = BASE_DETAIL_URL.format(external_id=external_id)
    try:
        resp = session.get(url, timeout=25)
        resp.raise_for_status()
    except Exception as e:
        print(f"  ⚠ Request lỗi [{external_id}]: {e}")
        return None

    nuxt_script = extract_nuxt_script(resp.text)
    if not nuxt_script:
        print(f"  ⚠ Không tìm thấy Nuxt State: {external_id}")
        return None

    try:
        raw = eval_nuxt_via_node(nuxt_script)
    except Exception as e:
        print(f"  ⚠ Node.js lỗi [{external_id}]: {e}")
        return None

    if "_node_error" in raw:
        print(f"  ⚠ Node error [{external_id}]: {raw['_node_error']}")
        return None

    payload = build_payload(raw, external_id)
    if payload is not None and not validate_payload(payload):
        print(f"  ⚠ Payload invalid [{external_id}]")
        return None
    return payload


# ─── Sync lên API ─────────────────────────────────────────────────────────────

def append_api_transport_error(chunk: list[dict], http_status: Optional[int], detail: str) -> None:
    """Ghi 1 dòng JSONL khi cả chunk không sync được lên API (tương tự log Pha A in console)."""
    record = {
        "at": datetime.now(timezone.utc).isoformat(),
        "endpoint": SYNC_DETAIL_API,
        "http_status": http_status,
        "external_ids": [p.get("external_id") for p in chunk if isinstance(p, dict)],
        "detail": (detail or "")[:4000],
    }
    try:
        with open(DETAIL_API_ERROR_LOG, "a", encoding="utf-8") as f:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")
    except OSError as e:
        print(f"  [warn] Không ghi được {DETAIL_API_ERROR_LOG}: {e}")


def load_id_set(path: str) -> set[str]:
    if not os.path.exists(path):
        return set()
    try:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, list):
            return set(data)
        if isinstance(data, dict):
            if "ids" in data:
                if isinstance(data["ids"], dict):
                    return set(data["ids"].keys())
                elif isinstance(data["ids"], list):
                    return set(data["ids"])
            return set(data.keys())
        return set()
    except Exception:
        return set()


def save_id_set(path: str, ids: set[str]) -> None:
    now = datetime.now(timezone.utc).isoformat()
    payload = {
        "generated_at": now,
        "count": len(ids),
        "ids": {i: {"at": now} for i in sorted(ids)},
    }
    atomic_write_json(path, payload)


def write_done_summary(path: str, crawled_ids: set[str], synced_ids: set[str], crawl_failed_ids: list[str], sync_failed_ids: list[str], total_targets: int, elapsed_seconds: float) -> None:
    summary = {
        "run_at": datetime.now(timezone.utc).isoformat(),
        "elapsed_seconds": round(elapsed_seconds, 2),
        "total_targets": total_targets,
        "total_crawled": len(crawled_ids),
        "total_synced": len(synced_ids),
        "total_crawl_failed": len(crawl_failed_ids),
        "total_sync_failed": len(sync_failed_ids),
        "crawl_failed_ids": crawl_failed_ids,
        "sync_failed_ids": sync_failed_ids,
        "synced_not_crawled": sorted(synced_ids - crawled_ids),
        "crawled_not_synced": sorted(crawled_ids - synced_ids),
        "completed_ids": sorted(crawled_ids & synced_ids),
    }
    atomic_write_json(path, summary)


def sync_to_api(payloads: list[dict], token: str) -> list[str]:
    """POST từng chunk; trả về external_id đã sync HTTP thành công (ack thật, không nhầm với crawl)."""
    synced_ids: list[str] = []
    if not payloads:
        return synced_ids
    session = requests.Session()
    session.headers.update({
        "Content-Type":  "application/json",
        "Authorization": f"Bearer {token}",
    })
    total = len(payloads)
    for i in range(0, total, CHUNK_SIZE):
        chunk = payloads[i:i + CHUNK_SIZE]
        success = False
        last_status: Optional[int] = None
        last_body = ""
        last_exc: Optional[str] = None
        for attempt in range(1, 4):
            try:
                resp = session.post(SYNC_DETAIL_API, json=chunk, timeout=60)
                last_status = resp.status_code
                last_body = resp.text or ""
                if resp.status_code in (200, 201):
                    print(f"  [sync] Chunk {i+1}~{i+len(chunk)}/{total} → OK")
                    try:
                        body = resp.json()
                        stats = body.get("data") or body
                        tr = stats.get("totalReceived")
                        ts = stats.get("totalSuccessful")
                        tf = stats.get("totalFailed")
                        if tr is not None:
                            print(
                                f"  [sync] API: totalReceived={tr} totalSuccessful={ts} "
                                f"totalFailed={tf} (lỗi từng dòng → bảng fco_detail_sync_errors)"
                            )
                    except Exception:
                        pass
                    success = True
                    for p in chunk:
                        eid = p.get("external_id")
                        if eid:
                            synced_ids.append(eid)
                    break
                print(f"  [sync] Thử {attempt}: HTTP {resp.status_code} — {last_body[:200]}")
            except Exception as e:
                last_exc = repr(e)
                print(f"  [sync] Thử {attempt}: lỗi mạng — {e}")
            time.sleep(2)
        if not success:
            detail = last_exc if last_exc else f"HTTP {last_status}: {last_body[:2000]}"
            print(f"  ❌ Chunk {i+1}~{i+len(chunk)} thất bại sau 3 lần — ghi {DETAIL_API_ERROR_LOG}")
            append_api_transport_error(chunk, last_status, detail)
    return synced_ids


# ─── Main ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="FCO Hub — Pha B: Crawl chi tiết cầu thủ")
    parser.add_argument("--resume",  action="store_true",
                        help="Bỏ qua external_id đã sync (detail_synced.json)")
    parser.add_argument("--no-sync", action="store_true",
                        help="Chỉ crawl và in kết quả, không gửi lên API")
    parser.add_argument("--limit",   type=int, default=0,
                        help="Giới hạn số cầu thủ (0 = không giới hạn, dùng để test)")
    args = parser.parse_args()

    if not os.path.exists(INPUT_FILE):
        print(f"❌ Không tìm thấy {INPUT_FILE}. Hãy chạy main_crawler.py trước.")
        sys.exit(1)

    with open(INPUT_FILE, encoding="utf-8") as f:
        phase_a = json.load(f)

    all_ids = list({p["external_id"] for p in phase_a if p.get("external_id")})
    print(f"[info] Tổng external_id duy nhất từ Pha A: {len(all_ids)}")

    crawled_ids = load_id_set(CRAWLED_FILE)
    synced_ids = load_id_set(SYNCED_FILE)

    print(f"[info] Đã crawl trước đó: {len(crawled_ids)}")
    print(f"[info] Đã sync API trước đó: {len(synced_ids)}")

    if args.resume:
        targets = [i for i in all_ids if i not in synced_ids]
    else:
        targets = all_ids
    if args.limit > 0:
        targets = targets[:args.limit]
        print(f"[info] Giới hạn test: {args.limit} cầu thủ")

    print(f"[info] Sẽ crawl: {len(targets)} cầu thủ\n")

    token = None
    if not args.no_sync:
        token = get_admin_token()

    session     = create_session()
    results     = []
    crawl_failed_ids  = []
    sync_failed_ids = []
    start       = time.time()
    api_sync_failed = False


    for idx, ext_id in enumerate(targets, 1):
        print(f"[{idx:>4}/{len(targets)}] {ext_id}...", end=" ", flush=True)

        payload = crawl_one(session, ext_id)
        if payload:
            results.append(payload)
            crawled_ids.add(ext_id)
            cd = payload["card_detail"]
            lp_str = f"LivePerf+{cd['liveperf']}" if cd["has_live_perf"] else "NoLivePerf"
            print(f"✓ {lp_str} | Traits={len(payload['traits'])} | Prices={len(payload['prices'])}")
        else:
            crawl_failed_ids.append(ext_id)
            print("✗")

        if idx % CHECKPOINT_EVERY == 0:
            save_id_set(CRAWLED_FILE, crawled_ids)
            print(f"  [checkpoint] Đã lưu {len(crawled_ids)} crawled ids")

        # ── Sync API theo batch ──────────────────────────────────────────
        if not args.no_sync and token and len(results) >= CHUNK_SIZE:
            chunk_synced = sync_to_api(results, token)
            if len(chunk_synced) < len(results):
                api_sync_failed = True
                chunk_synced_set = set(chunk_synced)
                sync_failed_ids.extend([p.get("external_id") for p in results if p.get("external_id") not in chunk_synced_set])
            synced_ids.update(chunk_synced)
            save_id_set(SYNCED_FILE, synced_ids)
            results = []

        time.sleep(DELAY_SECONDS)

    # Sync phần còn lại lên API
    if not args.no_sync and token and results:
        chunk_synced = sync_to_api(results, token)
        if len(chunk_synced) < len(results):
            api_sync_failed = True
            chunk_synced_set = set(chunk_synced)
            sync_failed_ids.extend([p.get("external_id") for p in results if p.get("external_id") not in chunk_synced_set])
        synced_ids.update(chunk_synced)
        save_id_set(SYNCED_FILE, synced_ids)

    save_id_set(CRAWLED_FILE, crawled_ids)
    save_id_set(SYNCED_FILE, synced_ids)
    write_done_summary(DONE_FILE, crawled_ids, synced_ids, crawl_failed_ids, sync_failed_ids, len(targets), time.time() - start)

    elapsed = time.time() - start
    print(f"\n{'='*55}")
    print(f"[XONG] {elapsed:.0f}s | crawled={len(crawled_ids)} | synced={len(synced_ids)} | crawl_fail={len(crawl_failed_ids)} | sync_fail={len(sync_failed_ids)}")
    if crawl_failed_ids:
        print(f"IDs crawl lỗi: {crawl_failed_ids[:10]}{'...' if len(crawl_failed_ids) > 10 else ''}")
    if sync_failed_ids:
        print(f"IDs sync lỗi: {sync_failed_ids[:10]}{'...' if len(sync_failed_ids) > 10 else ''}")
    print(f"{'='*55}")

    if api_sync_failed or crawl_failed_ids or sync_failed_ids:
        sys.exit(1)


if __name__ == "__main__":
    main()
