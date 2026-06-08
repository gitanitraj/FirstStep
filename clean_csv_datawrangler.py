import argparse
import re
from pathlib import Path

import pandas as pd


def normalize_column_names(columns: pd.Index) -> pd.Index:
    """Normalize column names to snake_case and strip whitespace."""
    normalized = []
    for col in columns:
        if isinstance(col, str):
            col = col.strip()
            col = re.sub(r"[\s]+", "_", col)
            col = re.sub(r"[^0-9a-zA-Z_]+", "_", col)
            col = re.sub(r"__+", "_", col)
            col = col.strip("_")
            normalized.append(col.lower())
        else:
            normalized.append(col)
    return pd.Index(normalized)


def clean_string_columns(df: pd.DataFrame) -> pd.DataFrame:
    """Trim whitespace and normalize empty strings to NaN."""
    string_cols = df.select_dtypes(include=["object", "string"]).columns
    for col in string_cols:
        df[col] = df[col].astype("string").str.strip()
        df[col] = df[col].replace({"": pd.NA})
    return df


def fill_missing_values(df: pd.DataFrame) -> pd.DataFrame:
    """Fill missing values for numeric and categorical columns."""
    numeric_cols = df.select_dtypes(include=["number"]).columns
    for col in numeric_cols:
        if df[col].isna().any():
            median_value = df[col].median(skipna=True)
            if pd.notna(median_value):
                df[col] = df[col].fillna(median_value)

    categorical_cols = df.select_dtypes(include=["category", "object", "string"]).columns
    for col in categorical_cols:
        if df[col].isna().any():
            mode_values = df[col].mode(dropna=True)
            if not mode_values.empty:
                df[col] = df[col].fillna(mode_values.iloc[0])

    return df


def clean_csv(input_path: str, output_path: str) -> None:
    """Load a CSV, clean it, and write a cleaned file."""
    input_path = Path(input_path)
    if not input_path.exists():
        raise FileNotFoundError(f"Input file not found: {input_path}")

    df = pd.read_csv(input_path)

    df.columns = normalize_column_names(df.columns)
    df = df.drop_duplicates()
    df = df.dropna(how="all")
    df = clean_string_columns(df)
    df = df.dropna(axis=1, how="all")
    df = fill_missing_values(df)

    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(output_path, index=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Clean a CSV file with Microsoft-style Data Wrangler cleaning steps.")
    parser.add_argument("input", help="Path to the input CSV file")
    parser.add_argument("output", help="Path for the cleaned CSV output file")
    args = parser.parse_args()

    clean_csv(args.input, args.output)
