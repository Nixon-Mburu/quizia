import requests

BACKEND_URL = "http://localhost:8081/api/questions?topic=Science%20%26%20Technology"

def test_questions_endpoint():
    print(f"Testing {BACKEND_URL}")
    try:
        resp = requests.get(BACKEND_URL)
        print(f"Status: {resp.status_code}")
        print(f"Content-Type: {resp.headers.get('Content-Type')}")
        print(f"Length: {len(resp.text)}")
        print(f"First 500 chars:\n{resp.text[:500]}")
        assert resp.status_code == 200, f"Expected 200, got {resp.status_code}"
        data = resp.json()
        assert isinstance(data, list), "Response is not a list"
        assert len(data) > 0, "No questions returned"
        print(f"Sample question: {data[0]}")
    except Exception as e:
        print(f"Error: {e}")
        raise

if __name__ == "__main__":
    test_questions_endpoint()
