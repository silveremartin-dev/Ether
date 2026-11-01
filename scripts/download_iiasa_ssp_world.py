import pyam
import sys
import os

def main():
    print("Connecting to IIASA SSP Scenario Database via pyam...")
    variables = [
        "Population",
        "GDP|PPP",
        "Primary Energy",
        "Emissions|CO2",
        "Temperature|Global Mean",
        "Land Cover|Cropland",
        "Land Cover|Forest",
    ]
    try:
        df = pyam.read_iiasa(
            "ssp",
            region="World",
            variable=variables
        )
        out_csv = "data/sources/iiasa_ssp/iiasa_ssp_world_full.csv"
        os.makedirs(os.path.dirname(out_csv), exist_ok=True)
        df.to_csv(out_csv)
        print(f"Successfully exported {len(df)} records to {out_csv}")
    except Exception as e:
        print(f"Error reading IIASA: {e}", file=sys.stderr)
        # Try broader query or specific models
        try:
            print("Attempting query for standard SSP models (MESSAGE-GLOBIOM, REMIND-MAGPIE, IMAGE, GCAM4)...")
            df = pyam.read_iiasa("ssp", region="World")
            out_csv = "data/sources/iiasa_ssp/iiasa_ssp_world_full.csv"
            df.to_csv(out_csv)
            print(f"Successfully exported {len(df)} records to {out_csv}")
        except Exception as e2:
            print(f"Fallback query error: {e2}", file=sys.stderr)
            sys.exit(1)

if __name__ == "__main__":
    main()
