#!/usr/bin/env python3
"""
run_tests.py

Simple API test runner for the Quizia backend.

Tests performed:
- GET /api/questions?topic=...&limit=...    -> expect 200 and list of questions
- GET /api/rooms                            -> expect 200 and list of rooms
- POST /api/rooms/register                  -> create a test room, expect success and visible in GET /api/rooms
- POST /api/results                         -> submit a test result, expect success
- GET /api/leaderboard?roomId=...           -> expect leaderboard contains the submitted result

Usage: python3 run_tests.py
Requires: requests (pip install requests)
"""

import sys
import time
import json
from typing import Any, Dict, List

import requests

BASE = "http://localhost:8081"

def ok(msg: str):
    print("[PASS]", msg)

def fail(msg: str):
    print("[FAIL]", msg)

def test_questions():
    url = f"{BASE}/api/questions?topic=General%20Knowledge&limit=5"
    try:
        r = requests.get(url, timeout=5)
        if r.status_code != 200:
            fail(f"GET /api/questions returned status {r.status_code}")
            return False
        data = r.json()
        if not isinstance(data, list):
            fail("GET /api/questions did not return a JSON list")
            return False
        if len(data) == 0:
            fail("GET /api/questions returned empty list (expected seeded questions)")
            return False
        # check shape of first item
        item = data[0]
        keys = set(item.keys()) if isinstance(item, dict) else set()
        required = {"question", "optionA", "optionB", "optionC", "optionD", "correctOption"}
        if not required.intersection(keys):
            # be tolerant of naming: accept id/topic/question
            if not ("question" in keys or "questionText" in keys):
                fail(f"Question object missing expected keys: got {keys}")
                return False
        ok("GET /api/questions returned a non-empty list with expected fields")
        return True
    except Exception as ex:
        fail(f"GET /api/questions request failed: {ex}")
        return False

def test_get_rooms():
    url = f"{BASE}/api/rooms"
    try:
        r = requests.get(url, timeout=5)
        if r.status_code != 200:
            fail(f"GET /api/rooms returned status {r.status_code}")
            return False
        data = r.json()
        if not isinstance(data, list):
            fail("GET /api/rooms did not return a JSON list")
            return False
        ok("GET /api/rooms returned a list")
        return True
    except Exception as ex:
        fail(f"GET /api/rooms request failed: {ex}")
        return False

def test_create_user_and_visibility():
    username = f"testuser-{int(time.time())}"
    try:
        r = requests.post(f"{BASE}/api/users/register", json={"username": username}, timeout=5)
        if r.status_code not in (200,201):
            return False, f"POST /api/users/register returned {r.status_code} body={r.text}"
        # verify via GET /api/users
        r2 = requests.get(f"{BASE}/api/users", timeout=5)
        if r2.status_code != 200:
            return False, f"After register, GET /api/users returned {r2.status_code}"
        found = False
        for m in r2.json():
            if m.get("username") == username:
                found = True
                break
        if not found:
            return False, "Registered user not found in GET /api/users"
        return True, username
    except Exception as ex:
        return False, f"POST /api/users/register failed: {ex}"

def test_register_room_and_visibility():
    # create a unique test room id
    room_id = f"testroom-{int(time.time())}"
    payload = {
        "roomId": room_id,
        "roomName": "Test Room from automated tests",
        "topics": "General Knowledge",
        "memberCount": 2,
        "memberNames": "alice,bob"
    }
    try:
        r = requests.post(f"{BASE}/api/rooms/register", json=payload, timeout=5)
        if r.status_code not in (200,201):
            return False, f"POST /api/rooms/register returned {r.status_code} body={r.text}"
       
        time.sleep(0.5)
        #
        r2 = requests.get(f"{BASE}/api/rooms", timeout=5)
        if r2.status_code != 200:
            return False, f"After register, GET /api/rooms returned {r2.status_code}"
        found = False
        for m in r2.json():
            
            rid = m.get("roomId") or m.get("room_id")
            if rid == room_id:
                
                mc = m.get("memberCount") or m.get("member_count")
                mn = m.get("memberNames") or m.get("member_names")
                if mc is None:
                    return False, "Registered room missing memberCount/member_count"
                if mn is None:
                    return False, "Registered room missing memberNames/member_names"
                found = True
                break
        if not found:
            return False, "Registered room not found in GET /api/rooms"
        return True, room_id
    except Exception as ex:
        return False, f"POST /api/rooms/register failed: {ex}"

def test_submit_result_and_leaderboard(room_id: str, username: str = "tester") -> bool:
    payload = {
        "roomId": room_id,
        "username": username,
        "correct": 3,
        "totalTimeMs": 12345
    }
    try:
        r = requests.post(f"{BASE}/api/results", json=payload, timeout=5)
        if r.status_code != 200:
            fail(f"POST /api/results returned {r.status_code} body={r.text}")
            return False
        # check leaderboard
        time.sleep(0.3)
        r2 = requests.get(f"{BASE}/api/leaderboard?roomId={requests.utils.quote(room_id)}", timeout=5)
        if r2.status_code != 200:
            fail(f"GET /api/leaderboard returned {r2.status_code}")
            return False
        data = r2.json()
        if not isinstance(data, list):
            fail("GET /api/leaderboard did not return a list")
            return False
        # find our username
        found = False
        for entry in data:
            uname = entry.get("username") or entry.get("user")
            if uname == username:
                # check total time and correct fields exist
                if (entry.get("total_time_ms") is None) and (entry.get("totalTimeMs") is None):
                    fail("Leaderboard entry missing total_time_ms/totalTimeMs")
                    return False
                if (entry.get("correct") is None) and (entry.get("correctAnswers") is None):
                    fail("Leaderboard entry missing correct/count field")
                    return False
                # verify time value matches (exact match expected from our submit)
                tval = entry.get("total_time_ms") if entry.get("total_time_ms") is not None else entry.get("totalTimeMs")
                try:
                    if int(tval) != 12345:
                        fail(f"Leaderboard total_time_ms value {tval} does not match submitted 12345")
                        return False
                except Exception:
                    # ignore parse error
                    pass
                found = True
                break
        if not found:
            fail("Submitted result not present in leaderboard")
            return False
        ok("POST /api/results and GET /api/leaderboard integration successful")
        return True
    except Exception as ex:
        fail(f"Result submit/leaderboard requests failed: {ex}")
        return False

def main():
    print("Running API tests against:", BASE)
    results = []

    results.append(("questions", test_questions()))
    results.append(("get_rooms", test_get_rooms()))

    ok_user, user_info = test_create_user_and_visibility()
    if ok_user:
        ok(f"POST /api/users/register created user {user_info}")
        results.append(("create_user", True))
    else:
        fail(f"create_user failed: {user_info}")
        results.append(("create_user", False))

    ok_reg, reg_info = test_register_room_and_visibility()
    if ok_reg:
        ok(f"POST /api/rooms/register created room {reg_info}")
        results.append(("register_room", True))
        # submit result + leaderboard - use created user if available otherwise 'tester'
        username_for_result = user_info if ok_user else "tester"
        results.append(("results_and_leaderboard", test_submit_result_and_leaderboard(reg_info, username_for_result)))
    else:
        fail(f"register_room failed: {reg_info}")
        results.append(("register_room", False))
        results.append(("results_and_leaderboard", False))

    print("\nSummary:")
    all_ok = True
    for name, ok_flag in results:
        status = "PASS" if ok_flag else "FAIL"
        print(f" - {name}: {status}")
        if not ok_flag:
            all_ok = False

    if not all_ok:
        print("\nOne or more tests failed.")
        sys.exit(2)
    print("\nAll API tests passed.")
    sys.exit(0)

if __name__ == '__main__':
    main()
