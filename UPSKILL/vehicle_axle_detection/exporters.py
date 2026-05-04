import json
import csv
import pandas as pd
from typing import List, Dict, Any
import os

def export_json(results: List[Dict[str, Any]], out_path: str):
    with open(out_path, 'w') as f:
        json.dump(results, f, indent=2)

def export_csv(results: List[Dict[str, Any]], out_path: str):
    if not results:
        return
    keys = results[0].keys()
    with open(out_path, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=keys)
        writer.writeheader()
        for row in results:
            writer.writerow(row)

def export_xlsx(results: List[Dict[str, Any]], out_path: str):
    df = pd.DataFrame(results)
    df.to_excel(out_path, index=False)
