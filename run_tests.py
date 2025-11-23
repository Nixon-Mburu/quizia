#!/usr/bin/env python3
"""
run_tests.py (enhanced)

Comprehensive API test runner for Quizia backend. This extends the original script
to vigorously test all API endpoints and common edge-cases.

Usage: python3 run_tests.py
Requires: requests (pip install requests)
"""

import sys
import time
import json
import threading
from typing import Any, Dict, List, Tuple

import requests

BASE = "http://localhost:8081"
TIMEOUT = 6

def ok(msg: str):
    print("[PASS]", msg)

def fail(msg: str):
    print("[FAIL]", msg)

def expect_2xx(r: requests.Response) -> bool:
    return 200 <= r.status_code < 300

def test_questions():
    url = f"{BASE}/api/questions?topic=General%20Knowledge&limit=5"
    try:
        r = requests.get(url, timeout=TIMEOUT)
        if not expect_2xx(r):
            fail(f"GET /api/questions returned status {r.status_code}")
            return False
        data = r.json()
        if not isinstance(data, list) or len(data) == 0:
            fail("GET /api/questions returned empty or non-list payload")
            return False
        # minimal shape check
        item = data[0]
        if not isinstance(item, dict) or not any(k in item for k in ("question", "questionText", "text")):
            fail(f"Question object missing text field: keys={list(item.keys()) if isinstance(item, dict) else type(item)}")
            return False
        ok("GET /api/questions returned seeded questions")
        return True
    except Exception as ex:
        fail(f"GET /api/questions request failed: {ex}")
        return False

def test_users_register_and_list():
    username = f"testuser_{int(time.time())}"
    try:
        r = requests.post(f"{BASE}/api/users/register", json={"username": username}, timeout=TIMEOUT)
        if not expect_2xx(r):
            fail(f"POST /api/users/register returned {r.status_code} body={r.text}")
            return False, None
        # register duplicate username (edge-case)
        rdup = requests.post(f"{BASE}/api/users/register", json={"username": username}, timeout=TIMEOUT)
        # backend may allow or reject duplicate; accept either 2xx or 4xx as handled
        if rdup.status_code >= 500:
            fail(f"Duplicate register produced server error {rdup.status_code}")
            return False, None
        # list users
        r2 = requests.get(f"{BASE}/api/users", timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"GET /api/users returned {r2.status_code}")
            return False, None
        users = r2.json()
        if not any(u.get("username") == username for u in users):
            fail("Newly registered user not present in GET /api/users")
            return False, None
        ok("User register and list endpoints functioning")
        return True, username
    except Exception as ex:
        fail(f"test_users_register_and_list failed: {ex}")
        return False, None

def test_register_room_and_bad_payloads():
    room_id = f"testroom_{int(time.time())}"
    payload = {
        "roomId": room_id,
        "roomName": "Auto Test Room",
        "topics": "General Knowledge,Science",
        "memberCount": 0,
        "memberNames": ""
    }
    try:
        r = requests.post(f"{BASE}/api/rooms/register", json=payload, timeout=TIMEOUT)
        if not expect_2xx(r):
            fail(f"POST /api/rooms/register returned {r.status_code} body={r.text}")
            return False, None
        # missing fields
        rbad = requests.post(f"{BASE}/api/rooms/register", json={"roomId": ""}, timeout=TIMEOUT)
        # backend should return 4xx or 2xx; treat 5xx as failure
        if rbad.status_code >= 500:
            fail(f"Malformed register produced server error {rbad.status_code}")
            return False, None
        # verify visibility
        time.sleep(0.2)
        r2 = requests.get(f"{BASE}/api/rooms", timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"GET /api/rooms after register returned {r2.status_code}")
            return False, None
        rooms = r2.json()
        if not any((m.get("roomId") or m.get("room_id")) == room_id for m in rooms):
            fail("Registered room not visible in GET /api/rooms")
            return False, None
        ok("Room register and basic validation checks passed")
        return True, room_id
    except Exception as ex:
        fail(f"test_register_room_and_bad_payloads failed: {ex}")
        return False, None

def test_room_join_and_duplicates(room_id: str, usernames: List[str]) -> bool:
    try:
        # join sequentially
        for u in usernames:
            r = requests.post(f"{BASE}/api/rooms/join", json={"roomId": room_id, "username": u}, timeout=TIMEOUT)
            if not expect_2xx(r):
                fail(f"POST /api/rooms/join for {u} returned {r.status_code}")
                return False
        # duplicate join
        rdup = requests.post(f"{BASE}/api/rooms/join", json={"roomId": room_id, "username": usernames[0]}, timeout=TIMEOUT)
        if rdup.status_code >= 500:
            fail(f"Duplicate join produced server error {rdup.status_code}")
            return False
        # join non-existent room should return 4xx (or handled gracefully)
        rnon = requests.post(f"{BASE}/api/rooms/join", json={"roomId": "no-such-room-xyz", "username": "u"}, timeout=TIMEOUT)
        if rnon.status_code >= 500:
            fail(f"Join non-existent room produced server error {rnon.status_code}")
            return False
        # check room listing shows members
        time.sleep(0.3)
        r2 = requests.get(f"{BASE}/api/rooms", timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"GET /api/rooms returned {r2.status_code} after joins")
            return False
        found = False
        for m in r2.json():
            rid = m.get("roomId") or m.get("room_id")
            if rid == room_id:
                found = True
                mc = m.get("memberCount") or m.get("member_count") or 0
                mn = m.get("memberNames") or m.get("member_names") or ""
                try:
                    count = int(mc)
                except Exception:
                    count = 0
                if count < len(usernames):
                    fail(f"memberCount {count} less than expected {len(usernames)}")
                    return False
                for u in usernames:
                    if u not in mn:
                        fail(f"username {u} missing from memberNames: {mn}")
                        return False
                break
        if not found:
            fail("Room not found after joins")
            return False
        ok("Joins (including duplicate/non-existent checks) behaved as expected")
        return True
    except Exception as ex:
        fail(f"test_room_join_and_duplicates failed: {ex}")
        return False

def test_room_start_permissions(room_id: str, registrar_user: str, other_user: str) -> bool:
    try:
        # attempt start by non-registrar
        r = requests.post(f"{BASE}/api/rooms/start", json={"roomId": room_id, "username": other_user}, timeout=TIMEOUT)
        # allow backend to either forbid (4xx) or allow (2xx); but server errors are failures
        if r.status_code >= 500:
            fail(f"POST /api/rooms/start produced server error {r.status_code}")
            return False
        # start by registrar - should be accepted
        r2 = requests.post(f"{BASE}/api/rooms/start", json={"roomId": room_id, "username": registrar_user}, timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"Registrar start returned {r2.status_code} body={r2.text}")
            return False
        ok("Room start permission checks OK (non-fatal differences allowed)")
        return True
    except Exception as ex:
        fail(f"test_room_start_permissions failed: {ex}")
        return False

def test_results_and_leaderboard(room_id: str, username: str) -> bool:
    try:
        # valid result
        payload = {"roomId": room_id, "username": username, "correct": 4, "totalTimeMs": 4321}
        r = requests.post(f"{BASE}/api/results", json=payload, timeout=TIMEOUT)
        if not expect_2xx(r):
            fail(f"POST /api/results returned {r.status_code} body={r.text}")
            return False
        # invalid result (negative time)
        rbad = requests.post(f"{BASE}/api/results", json={"roomId": room_id, "username": username, "correct": -1, "totalTimeMs": -999}, timeout=TIMEOUT)
        if rbad.status_code >= 500:
            fail(f"Invalid result produced server error {rbad.status_code}")
            return False
        time.sleep(0.2)
        # get leaderboard
        r2 = requests.get(f"{BASE}/api/leaderboard?roomId={requests.utils.quote(room_id)}", timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"GET /api/leaderboard returned {r2.status_code}")
            return False
        data = r2.json()
        if not isinstance(data, list):
            fail("Leaderboard response is not a list")
            return False
        # find our username
        found = False
        for e in data:
            uname = e.get("username") or e.get("user")
            if uname == username:
                # presence is enough; check sorting later in more thorough tests
                found = True
                break
        if not found:
            fail("Submitted result not present in leaderboard")
            return False
        ok("Results submission and leaderboard retrieval OK")
        return True
    except Exception as ex:
        fail(f"test_results_and_leaderboard failed: {ex}")
        return False

def test_sql_injection_resilience():
  
    malicious = "'; DROP TABLE users;--"
    try:
        r = requests.post(f"{BASE}/api/users/register", json={"username": malicious}, timeout=TIMEOUT)
        if r.status_code >= 500:
            fail(f"SQL-injection-like payload caused server error {r.status_code}")
            return False
        # verify users table still present by listing users
        r2 = requests.get(f"{BASE}/api/users", timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"GET /api/users returned {r2.status_code} after injection attempt")
            return False
        ok("SQL-injection-like payload did not break user listing")
        return True
    except Exception as ex:
        fail(f"test_sql_injection_resilience failed: {ex}")
        return False

def test_parallel_joins(room_id: str, base_username: str, count: int = 8) -> bool:
    
    def join_one(u):
        try:
            r = requests.post(f"{BASE}/api/rooms/join", json={"roomId": room_id, "username": u}, timeout=TIMEOUT)
            if not expect_2xx(r):
                print(f"[PARALLEL JOIN FAIL] {u} -> {r.status_code}")
                return False
            return True
        except Exception as ex:
            print(f"[PARALLEL JOIN EX] {u} -> {ex}")
            return False
    threads = []
    results = []
    for i in range(count):
        uname = f"{base_username}_{i}_{int(time.time())}"
        t = threading.Thread(target=lambda u=uname, res=results: res.append(join_one(u)))
        threads.append(t)
        t.start()
    for t in threads:
        t.join()
    if not all(results):
        fail("Some parallel joins failed")
        return False
    ok("Parallel joins completed without server errors")
    return True

def test_five_joins_and_member_names(room_id: str) -> bool:
    """Register five users joining the given room and verify their names appear in memberNames."""
    try:
        joiners = [f"five_{int(time.time())}_{i}" for i in range(5)]
        # join sequentially
        for u in joiners:
            r = requests.post(f"{BASE}/api/rooms/join", json={"roomId": room_id, "username": u}, timeout=TIMEOUT)
            if not expect_2xx(r):
                fail(f"POST /api/rooms/join for {u} returned {r.status_code} body={r.text}")
                return False
        time.sleep(0.4)
        r2 = requests.get(f"{BASE}/api/rooms", timeout=TIMEOUT)
        if not expect_2xx(r2):
            fail(f"GET /api/rooms returned {r2.status_code} after five joins")
            return False
        found = False
        for m in r2.json():
            rid = m.get("roomId") or m.get("room_id")
            if rid == room_id:
                found = True
                mn = m.get("memberNames") or m.get("member_names") or ""
                missing = [u for u in joiners if u not in mn]
                if missing:
                    fail(f"The following joiners not found in memberNames: {missing} (memberNames='{mn}')")
                    return False
                # optionally check count
                mc = m.get("memberCount") or m.get("member_count") or 0
                try:
                    if int(mc) < 5:
                        fail(f"memberCount {mc} is less than 5 after five joins")
                        return False
                except Exception:
                    pass
                break
        if not found:
            fail("Room not found when verifying five joins")
            return False
        ok("Five joins recorded and names visible in memberNames column")
        return True
    except Exception as ex:
        fail(f"test_five_joins_and_member_names failed: {ex}")
        return False

def test_start_broadcast(room_id: str, registrar_user: str) -> bool:
    """Subscribe to SSE events for the room, then trigger start and verify subscribers receive it."""
    import requests
    received = {"ok": False}
    try:
        url = f"{BASE}/api/rooms/{requests.utils.quote(room_id)}/events"
        s = requests.get(url, stream=True, timeout=TIMEOUT)
        if s.status_code < 200 or s.status_code >= 300:
            fail(f"Could not subscribe to SSE endpoint: {s.status_code}")
            return False

        def reader(resp, state):
            try:
                for line in resp.iter_lines(decode_unicode=True):
                    if line is None:
                        continue
                    ln = line.strip()
                    if ln.startswith("data:") and ("start" in ln.lower() or "started" in ln.lower()):
                        state["ok"] = True
                        break
                    if ln.startswith("event:") and "start" in ln.lower():
                        state["ok"] = True
                        break
            except Exception:
                pass

        t = threading.Thread(target=reader, args=(s, received), daemon=True)
        t.start()

        # give reader a moment to connect
        time.sleep(0.2)
        # trigger start
        r = requests.post(f"{BASE}/api/rooms/start", json={"roomId": room_id, "username": registrar_user}, timeout=TIMEOUT)
        if r.status_code >= 500:
            fail(f"POST /api/rooms/start returned server error {r.status_code}")
            return False

        # wait up to TIMEOUT seconds for event
        waited = 0.0
        interval = 0.1
        while waited < TIMEOUT:
            if received.get("ok"):
                ok("Start broadcast received by SSE subscriber")
                try:
                    s.close()
                except Exception:
                    pass
                return True
            time.sleep(interval)
            waited += interval
        fail("Timed out waiting for start event via SSE")
        try:
            s.close()
        except Exception:
            pass
        return False
    except Exception as ex:
        fail(f"test_start_broadcast failed: {ex}")
        return False

def main():
    print("Running comprehensive API tests against:", BASE)
    summary: List[Tuple[str, bool]] = []

    summary.append(("questions", test_questions()))

    ok_users, uname = test_users_register_and_list()
    summary.append(("users_register_and_list", ok_users))

    ok_room, room_id = test_register_room_and_bad_payloads()
    summary.append(("register_room", ok_room))

    if ok_room and uname:
        # create extra joiner names
        joiners = [f"joinerA_{int(time.time())}", f"joinerB_{int(time.time())+1}"]
        summary.append(("room_joins", test_room_join_and_duplicates(room_id, joiners)))
        summary.append(("room_start_perms", test_room_start_permissions(room_id, uname, joiners[0])))
        summary.append(("results_leaderboard", test_results_and_leaderboard(room_id, uname)))
        summary.append(("parallel_joins", test_parallel_joins(room_id, "pj", 6)))
        # five joins visibility check
        summary.append(("five_joins_memberNames", test_five_joins_and_member_names(room_id)))
        # start broadcast (SSE) check
        summary.append(("start_broadcast", test_start_broadcast(room_id, uname)))
    else:
        fail("Skipping join/start/results tests because room registration or user creation failed")
        summary.extend([("room_joins", False), ("room_start_perms", False), ("results_leaderboard", False), ("parallel_joins", False)])

    summary.append(("sql_injection", test_sql_injection_resilience()))

    print("\nSummary:")
    all_ok = True
    for name, ok_flag in summary:
        print(f" - {name}: {'PASS' if ok_flag else 'FAIL'}")
        if not ok_flag:
            all_ok = False

    if not all_ok:
        print("\nOne or more tests failed.")
        sys.exit(2)
    print("\nAll API tests passed.")
    sys.exit(0)

if __name__ == '__main__':
    main()
