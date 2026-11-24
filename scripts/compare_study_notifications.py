#!/usr/bin/env python3
"""
Compares raw captured notifications (hearit_raw_debug.db) against HearIt history (hearit_history.db)
to identify exactly which notifications were announced, which were skipped, and which were dropped/missing.

Usage:
  python3 scripts/compare_study_notifications.py [--raw path/to/hearit_raw_debug.db] [--history path/to/hearit_history.db]
  python3 scripts/compare_study_notifications.py --pull [device_serial]
"""

import sys
import os
import argparse
import sqlite3
import datetime
import json
import subprocess

def parse_args():
    parser = argparse.ArgumentParser(description="Analyze 2-day HearIt notification study.")
    parser.add_argument("--raw", help="Path to hearit_raw_debug.db", default=None)
    parser.add_argument("--history", help="Path to hearit_history.db", default=None)
    parser.add_argument("--pull", nargs="?", const="", help="Pull databases directly from connected adb device")
    parser.add_argument("--clear-study", action="store_true", help="Clear raw study database on connected device")
    return parser.parse_args()

def pull_databases(serial=None):
    cmd_prefix = ["adb"]
    if serial:
        cmd_prefix.extend(["-s", serial])
    
    tmp_dir = "/tmp/hearit_study"
    os.makedirs(tmp_dir, exist_ok=True)
    raw_path = os.path.join(tmp_dir, "hearit_raw_debug.db")
    hist_path = os.path.join(tmp_dir, "hearit_history.db")
    
    print("Pulling hearit_raw_debug.db via adb...")
    subprocess.run(
        f"{' '.join(cmd_prefix)} exec-out run-as com.jayanth.hearit cat databases/hearit_raw_debug.db > {raw_path}",
        shell=True, check=True
    )
    print("Pulling hearit_history.db via adb...")
    subprocess.run(
        f"{' '.join(cmd_prefix)} exec-out run-as com.jayanth.hearit cat databases/hearit_history.db > {hist_path}",
        shell=True, check=True
    )
    subprocess.run(
        f"{' '.join(cmd_prefix)} exec-out run-as com.jayanth.hearit cat databases/hearit_history.db-wal > {hist_path}-wal",
        shell=True
    )
    subprocess.run(
        f"{' '.join(cmd_prefix)} exec-out run-as com.jayanth.hearit cat databases/hearit_history.db-shm > {hist_path}-shm",
        shell=True
    )
    return raw_path, hist_path

def analyze(raw_path, hist_path):
    if not os.path.exists(raw_path):
        print(f"Error: Raw database not found at {raw_path}")
        sys.exit(1)
    if not os.path.exists(hist_path):
        print(f"Error: History database not found at {hist_path}")
        sys.exit(1)

    raw_conn = sqlite3.connect(raw_path)
    hist_conn = sqlite3.connect(hist_path)
    
    raw_c = raw_conn.cursor()
    hist_c = hist_conn.cursor()

    print("=" * 80)
    print(" HEARIT 2-DAY NOTIFICATION STUDY COMPARISON REPORT")
    print(f" Generated: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 80)

    # 1. Total Raw Events per package
    raw_c.execute("SELECT package_name, COUNT(*) FROM raw_notifications GROUP BY package_name")
    raw_counts = dict(raw_c.fetchall())
    total_raw = sum(raw_counts.values())
    print(f"\n📊 Total Raw Android Events Captured: {total_raw}")
    for pkg, count in sorted(raw_counts.items(), key=lambda x: -x[1]):
        print(f"  • {pkg}: {count}")

    # 2. History Events per package
    hist_c.execute("SELECT packageName, COUNT(*) FROM history_entries GROUP BY packageName")
    hist_counts = dict(hist_c.fetchall())
    print(f"\n📋 Total Recorded History Entries in DB: {sum(hist_counts.values())}")
    for pkg, count in sorted(hist_counts.items(), key=lambda x: -x[1]):
        print(f"  • {pkg}: {count}")

    # 3. Detailed per-event correlation
    print("\n" + "=" * 80)
    print(" EVENT-BY-EVENT PIPELINE AUDIT")
    print("=" * 80)

    query = """
    SELECT id, recorded_at_millis, post_time_millis, package_name, sbn_key,
           is_group, is_group_summary, is_ongoing, flags,
           android_title, android_text, android_big_text,
           extracted_sender, extracted_message_text
    FROM raw_notifications
    ORDER BY recorded_at_millis ASC
    """
    raw_c.execute(query)
    raw_rows = raw_c.fetchall()

    category_counts = {
        "MATCHED_HISTORY": 0,
        "GROUP_SUMMARY_SUPPRESSED": 0,
        "EMPTY_CONTENT_SUPPRESSED": 0,
        "WHATSAPP_ROOT_SUMMARY": 0,
        "POSSIBLE_DEDUP_WINDOW": 0,
        "ANOMALY_MISSING": 0,
    }

    missing_samples = []

    for r in raw_rows:
        (r_id, r_rec, r_post, r_pkg, r_key, r_is_grp, r_is_summary, r_ongoing, r_flags,
         r_title, r_text, r_big_text, r_ext_sender, r_ext_msg) = r

        dt_str = datetime.datetime.fromtimestamp(r_rec / 1000.0).strftime('%m-%d %H:%M:%S')
        body = (r_ext_msg or r_text or r_big_text or "").strip()
        sender = (r_ext_sender or r_title or "").strip()

        # Check if matched in history
        hist_c.execute(
            """
            SELECT id, status, reason, recordedAtMillis
            FROM history_entries
            WHERE packageName = ?
              AND (key = ? OR (messageText = ? AND messageText != ''))
              AND recordedAtMillis BETWEEN ? AND ?
            LIMIT 1
            """,
            (r_pkg, r_key, body, r_rec - 60000, r_rec + 60000)
        )
        match = hist_c.fetchone()

        if match:
            category_counts["MATCHED_HISTORY"] += 1
            status, reason = match[1], match[2]
            # print(f"[{dt_str}] ✅ {r_pkg} | {sender} -> MATCHED ({status}, reason={reason})")
        else:
            # Diagnose why it wasn't recorded
            if r_is_summary == 1:
                category_counts["GROUP_SUMMARY_SUPPRESSED"] += 1
            elif r_pkg == "com.whatsapp" and r_key == "com.whatsapp||1":
                category_counts["WHATSAPP_ROOT_SUMMARY"] += 1
            elif not body and not sender:
                category_counts["EMPTY_CONTENT_SUPPRESSED"] += 1
            else:
                category_counts["ANOMALY_MISSING"] += 1
                missing_samples.append({
                    "id": r_id,
                    "time": dt_str,
                    "pkg": r_pkg,
                    "key": r_key,
                    "sender": sender,
                    "body": body,
                    "is_group": r_is_grp,
                    "is_summary": r_is_summary,
                })

    print(f"\n🔍 Audit Classification Summary:")
    print(f"  ✅ Matched in History: {category_counts['MATCHED_HISTORY']}")
    print(f"  🛡️ Group Summary Suppressed (Standard Android behavior): {category_counts['GROUP_SUMMARY_SUPPRESSED']}")
    print(f"  🛡️ WhatsApp Root Summary Suppressed: {category_counts['WHATSAPP_ROOT_SUMMARY']}")
    print(f"  🛡️ Empty Content Suppressed: {category_counts['EMPTY_CONTENT_SUPPRESSED']}")
    print(f"  ⚠️ Missing / Needs Inspection: {category_counts['ANOMALY_MISSING']}")

    if missing_samples:
        print("\n" + "=" * 80)
        print(" ⚠️ MISSING NOTIFICATIONS SAMPLES (Sent by Android, but not in History):")
        print("=" * 80)
        for sample in missing_samples[:20]:
            print(f"• [{sample['time']}] {sample['pkg']}")
            print(f"    Key: {sample['key']}")
            print(f"    Sender: '{sample['sender']}' | Body: '{sample['body']}'")
            print(f"    Flags: is_group={sample['is_group']}, is_summary={sample['is_summary']}")
    else:
        print("\n✨ Zero unexplained missing notifications found!")

    raw_conn.close()
    hist_conn.close()

if __name__ == "__main__":
    args = parse_args()
    if args.clear_study:
        serial = args.pull if (args.pull is not None and args.pull != "") else None
        cmd_prefix = ["adb"]
        if serial:
            cmd_prefix.extend(["-s", serial])
        print("Clearing hearit_raw_debug.db on device...")
        subprocess.run(
            f"{' '.join(cmd_prefix)} shell 'run-as com.jayanth.hearit rm -f databases/hearit_raw_debug.db*'",
            shell=True, check=True
        )
        print("Cleared raw study database successfully.")
        if args.raw is None and args.history is None:
            sys.exit(0)

    if args.pull is not None:
        raw_p, hist_p = pull_databases(args.pull if args.pull != "" else None)
    else:
        raw_p = args.raw or "/tmp/hearit_raw_debug.db"
        hist_p = args.history or "/tmp/hearit_history.db"
    analyze(raw_p, hist_p)
