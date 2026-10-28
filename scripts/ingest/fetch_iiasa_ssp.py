#!/usr/bin/env python3
"""
IIASA Shared Socioeconomic Pathways (SSP) Data Ingestion Script.

Queries future socioeconomic scenarios (SSP1, SSP2, SSP3, SSP4, SSP5)
directly from the IIASA ECE Scenario Database via the official 'pyam' or 'ixmp4' packages.

API Endpoint: https://ssp.apps.ece.iiasa.ac.at/api
Requirements: pip install pyam-iamc ixmp4
"""

import sys
import json
from pathlib import Path

OUTPUT_DIR = Path("data/sources/predictive")

def fetch_ssp_data():
    try:
        import pyam
    except ImportError:
        print("[-] 'pyam' is not installed. Install it with: pip install pyam-iamc")
        print("    Alternatively install 'ixmp4': pip install ixmp4")
        return False

    print("[*] Connecting to IIASA Scenario Services for SSP Database...")
    try:
        # Query World-level indicators for SSP2 (Middle of the Road)
        df = pyam.read_iiasa(
            "ssp",
            region="World",
            scenario=["SSP2-Baseline", "SSP2-45", "SSP1-Baseline", "SSP5-Baseline"],
            variable=[
                "Population",
                "GDP|PPP",
                "Primary Energy",
                "Emissions|CO2"
            ]
        )
        print(f"[+] Successfully fetched {len(df)} records from IIASA SSP.")
        
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        csv_out = OUTPUT_DIR / "iiasa_ssp_world_scenarios.csv"
        df.to_csv(csv_out)
        print(f"[+] Saved raw IIASA SSP series to: {csv_out}")
        return True
    except Exception as e:
        print(f"[-] IIASA API query failed: {e}")
        return False

if __name__ == "__main__":
    fetch_ssp_data()
