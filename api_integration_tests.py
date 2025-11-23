"""
Comprehensive integration tests for the Quizia backend.

Usage:
  pip install requests
  python3 api_integration_tests.py

This script will:
- Register several users
- Register a room with multiple topics
- Have multiple users join the room
- Subscribe to Server-Sent Events (SSE) for the room in a background thread
- Start the room (trigger SSE "start")
- Fetch questions for a topic
- Submit a sample result
- Fetch the leaderboard
- Call admin reload DB endpoint

It prints detailed results for each step.
"""

import requests
import threading
import time
import json
import sys
import uuid

BASE = "http://localhost:8081"
TIMEOUT = 5

# Helper print
def info(msg):
    print("[INFO] " + msg)

def err(msg):
    print("[ERROR] " + msg, file=sys.stderr)

# SSE listener (minimal, using requests streaming)
def sse_listener(room_id, stop_event):
    url = f"{BASE}/api/rooms/{room_id}/events"
    info(f"SSE listener connecting to {url}")
    try:
        with requests.get(url, stream=True, timeout=(5, 60)) as resp:
            info(f"SSE connection status: {resp.status_code}")
            if resp.status_code != 200:
                err(f"SSE subscribe failed: {resp.status_code} {resp.text[:200]}")
                return
            # iterate over streamed lines
            for line in resp.iter_lines(decode_unicode=True):
                if stop_event.is_set():
                    info("SSE listener stopping")
                    break
                if line is None:
                    continue
                line = line.strip()
                if not line:
                    continue
                # SSE 'event:' or 'data:' lines
                if line.startswith("event:"):
                    event = line[len("event:"):].strip()
                    info(f"SSE event: {event}")
                elif line.startswith("data:"):
                    data = line[len("data:"):].strip()
                    info(f"SSE data: {data}")
                else:
                    info(f"SSE line: {line}")
    except Exception as ex:
        err(f"SSE listener exception: {ex}")


def register_user(username):
    url = f"{BASE}/api/users/register"
    payload = {"username": username}
    info(f"Registering user {username}")
    r = requests.post(url, json=payload, timeout=TIMEOUT)
    info(f" -> {r.status_code} {r.text[:200]}")
    return r


def list_users():
    url = f"{BASE}/api/users"
    info("Listing users")
    r = requests.get(url, timeout=TIMEOUT)
    info(f" -> {r.status_code}")
    if r.status_code == 200:
        try:
            data = r.json()
            info(f" -> {len(data)} users returned")
            print(json.dumps(data, indent=2))
            return data
        except Exception as ex:
            err(f"Failed to parse users JSON: {ex}")
    else:
        err(r.text)
    return None


def register_room(room_id, room_name, created_by, topics):
    url = f"{BASE}/api/rooms/register"
    payload = {
        "roomId": room_id,
        "roomName": room_name,
        "createdBy": created_by,
        "topics": topics
    }
    info(f"Registering room {room_id} '{room_name}'")
    r = requests.post(url, json=payload, timeout=TIMEOUT)
    info(f" -> {r.status_code} {r.text[:200]}")
    return r


def list_rooms():
    url = f"{BASE}/api/rooms"
    info("Listing rooms")
    r = requests.get(url, timeout=TIMEOUT)
    info(f" -> {r.status_code}")
    if r.status_code == 200:
        try:
            data = r.json()
            info(f" -> {len(data)} rooms returned")
            print(json.dumps(data, indent=2))
            return data
        except Exception as ex:
            err(f"Failed to parse rooms JSON: {ex}")
    else:
        err(r.text)
    return None


def join_room(room_id, username):
    url = f"{BASE}/api/rooms/join"
    payload = {"roomId": room_id, "username": username}
    info(f"User {username} joining room {room_id}")
    r = requests.post(url, json=payload, timeout=TIMEOUT)
    info(f" -> {r.status_code} {r.text[:200]}")
    return r


def start_room(room_id):
    url = f"{BASE}/api/rooms/start"
    payload = {"roomId": room_id}
    info(f"Starting room {room_id}")
    r = requests.post(url, json=payload, timeout=TIMEOUT)
    info(f" -> {r.status_code} {r.text[:200]}")
    return r


def fetch_questions(topic, limit=30):
    url = f"{BASE}/api/questions"
    params = {"topic": topic, "limit": limit}
    info(f"Fetching questions for topic '{topic}'")
    r = requests.get(url, params=params, timeout=TIMEOUT)
    info(f" -> {r.status_code} Content-Type: {r.headers.get('Content-Type')}")
    if r.status_code == 200:
        try:
            data = r.json()
            info(f" -> {len(data)} questions returned")
            # print a sample
            if len(data) > 0:
                info("Sample question:")
                print(json.dumps(data[0], indent=2))
            return data
        except Exception as ex:
            err(f"Failed to parse questions JSON: {ex}")
            err(r.text[:1000])
    else:
        err(r.text[:500])
    return None


def submit_result(room_id, username, correct, total_time_ms):
    url = f"{BASE}/api/results"
    payload = {
        "roomId": room_id,
        "username": username,
        "correct": correct,
        "totalTimeMs": total_time_ms
    }
    info(f"Submitting result for {username} in room {room_id}")
    r = requests.post(url, json=payload, timeout=TIMEOUT)
    info(f" -> {r.status_code} {r.text[:200]}")
    return r


def get_leaderboard(room_id):
    url = f"{BASE}/api/leaderboard"
    params = {"roomId": room_id}
    info(f"Fetching leaderboard for room {room_id}")
    r = requests.get(url, params=params, timeout=TIMEOUT)
    info(f" -> {r.status_code}")
    if r.status_code == 200:
        try:
            data = r.json()
            info(f" -> {len(data)} entries")
            print(json.dumps(data, indent=2))
            return data
        except Exception as ex:
            err(f"Failed to parse leaderboard JSON: {ex}")
    else:
        err(r.text)
    return None


def reload_db():
    url = f"{BASE}/api/admin/reload-db"
    info("Reloading DB from SQL file via admin endpoint")
    r = requests.post(url, timeout=TIMEOUT)
    info(f" -> {r.status_code} {r.text[:200]}")
    return r


def main():
    info("Starting integration tests against " + BASE)

    # Unique identifiers for tests
    uid = str(uuid.uuid4())[:8]
    room_id = "TEST-" + uid
    room_name = "Integration Test Room " + uid
    topics = "Science & Technology,General Knowledge"

    # Users to create
    users = [f"t_user_{uid}_alice", f"t_user_{uid}_bob", f"t_user_{uid}_charlie"]

    # 1) Register users
    for u in users:
        register_user(u)

    # 2) List users
    list_users()

    # 3) Register a room
    register_room(room_id, room_name, users[0], topics)

    # 4) List rooms and ensure our room appears
    rooms = list_rooms()
    found = any(r.get('roomId') == room_id or r.get('room_id') == room_id for r in (rooms or []))
    info(f"Room present after registration: {found}")

    # 5) Start SSE listener in background
    stop_event = threading.Event()
    sse_thread = threading.Thread(target=sse_listener, args=(room_id, stop_event), daemon=True)
    sse_thread.start()

    # allow SSE connect time
    time.sleep(1)

    # 6) Have users join the room
    for u in users:
        join_room(room_id, u)

    # 7) Fetch questions for a topic
    qlist = fetch_questions("Science & Technology")

    # 8) Submit results for users with dummy scores
    if qlist and len(qlist) > 0:
        # simulate different scores
        submit_result(room_id, users[0], correct=5, total_time_ms=30000)
        submit_result(room_id, users[1], correct=7, total_time_ms=25000)
        submit_result(room_id, users[2], correct=3, total_time_ms=40000)

    # 9) Get leaderboard (before/after start)
    get_leaderboard(room_id)

    # 10) Trigger start event (by registrar)
    start_room(room_id)

    # wait a little for SSE to show start event
    time.sleep(2)

    # 11) Final leaderboard check
    get_leaderboard(room_id)

    # 12) Reload DB
    reload_db()

    # stop SSE listener
    stop_event.set()
    sse_thread.join(timeout=2)

    info("Integration tests completed")


if __name__ == '__main__':
    main()
