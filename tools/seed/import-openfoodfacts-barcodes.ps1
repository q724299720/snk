param(
    [string]$InputCsv = "data/seed/openfoodfacts-barcodes-template.csv",
    [string]$OutputCsv = "data/seed/openfoodfacts-import-preview.csv",
    [string]$UserAgent = "snk-phase0-import/0.1 (contact: replace-me)",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$requiredHeaders = @(
    "barcode",
    "brand_hint",
    "category_hint",
    "subcategory_hint",
    "alias_hint",
    "tag_hint",
    "enabled"
)

function Assert-FileExists {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Input CSV not found: $Path"
    }
}

function Assert-Headers {
    param([object[]]$Rows)

    if (-not $Rows) {
        return
    }

    $headers = $Rows[0].PSObject.Properties.Name
    foreach ($header in $requiredHeaders) {
        if ($header -notin $headers) {
            throw "Missing required header: $header"
        }
    }
}

function Get-EnabledRows {
    param([object[]]$Rows)

    return @(
        $Rows | Where-Object {
            $enabled = "$($_.enabled)".Trim().ToLowerInvariant()
            $enabled -eq "true"
        }
    )
}

function Assert-Barcodes {
    param([object[]]$Rows)

    foreach ($row in $Rows) {
        $barcode = "$($row.barcode)".Trim()
        if ([string]::IsNullOrWhiteSpace($barcode)) {
            throw "Enabled row contains empty barcode."
        }

        if ($barcode -notmatch '^\d+$') {
            throw "Barcode must be numeric: $barcode"
        }
    }
}

function Get-FirstValue {
    param([object[]]$Values)

    foreach ($value in $Values) {
        if (-not [string]::IsNullOrWhiteSpace("$value")) {
            return "$value".Trim()
        }
    }

    return ""
}

function Join-Keywords {
    param([string[]]$Values)

    $seen = New-Object 'System.Collections.Generic.HashSet[string]'
    $result = New-Object System.Collections.Generic.List[string]

    foreach ($value in $Values) {
        if ([string]::IsNullOrWhiteSpace($value)) {
            continue
        }

        foreach ($token in ($value -split '[,|/\s]+')) {
            $clean = $token.Trim()
            if ([string]::IsNullOrWhiteSpace($clean)) {
                continue
            }

            if ($seen.Add($clean)) {
                $result.Add($clean) | Out-Null
            }
        }
    }

    return ($result -join ",")
}

function Convert-ToSeedRow {
    param(
        [object]$InputRow,
        [object]$Product
    )

    $name = Get-FirstValue @(
        $Product.product_name_zh,
        $Product.product_name,
        $Product.generic_name_zh,
        $Product.generic_name
    )

    if ([string]::IsNullOrWhiteSpace($name)) {
        return $null
    }

    $brand = Get-FirstValue @(
        $InputRow.brand_hint,
        $Product.brands
    )

    $category = Get-FirstValue @(
        $InputRow.category_hint,
        "零食"
    )

    $subcategory = Get-FirstValue @(
        $InputRow.subcategory_hint
    )

    $alias = Get-FirstValue @(
        $InputRow.alias_hint
    )

    $tags = Get-FirstValue @(
        $InputRow.tag_hint
    )

    $searchKeywords = Join-Keywords @(
        $name,
        $brand,
        $category,
        $subcategory,
        $alias,
        $Product.quantity
    )

    return [pscustomobject]@{
        name            = $name
        item_type       = "packaged_product"
        category        = $category
        subcategory     = $subcategory
        brand           = $brand
        barcode         = "$($InputRow.barcode)".Trim()
        alias           = $alias
        search_keywords = $searchKeywords
        tags            = $tags
        source          = "external_api"
        audit_status    = "approved"
    }
}

function Invoke-OpenFoodFactsLookup {
    param(
        [string]$Barcode,
        [string]$UserAgent
    )

    $headers = @{
        "User-Agent" = $UserAgent
    }

    $url = "https://world.openfoodfacts.org/api/v2/product/$Barcode.json"
    $response = Invoke-RestMethod -Uri $url -Headers $headers -Method Get

    if ($null -eq $response -or $response.status -ne 1 -or $null -eq $response.product) {
        return $null
    }

    return $response.product
}

Assert-FileExists -Path $InputCsv
$rows = @(Import-Csv -Path $InputCsv)
Assert-Headers -Rows $rows

$enabledRows = @(Get-EnabledRows -Rows $rows)
Assert-Barcodes -Rows $enabledRows

if ($DryRun) {
    [pscustomobject]@{
        Mode             = "DryRun"
        InputCsv         = $InputCsv
        OutputCsv        = $OutputCsv
        TotalRows        = $rows.Count
        EnabledRows      = $enabledRows.Count
        UniqueBarcodes   = @($enabledRows.barcode | Sort-Object -Unique).Count
        NetworkRequests  = 0
    } | ConvertTo-Json
    exit 0
}

$seedRows = New-Object System.Collections.Generic.List[object]
$seenBarcodes = New-Object 'System.Collections.Generic.HashSet[string]'

foreach ($row in $enabledRows) {
    $barcode = "$($row.barcode)".Trim()

    if (-not $seenBarcodes.Add($barcode)) {
        continue
    }

    $product = Invoke-OpenFoodFactsLookup -Barcode $barcode -UserAgent $UserAgent
    if ($null -eq $product) {
        continue
    }

    $seedRow = Convert-ToSeedRow -InputRow $row -Product $product
    if ($null -ne $seedRow) {
        $seedRows.Add($seedRow) | Out-Null
    }
}

$outputDir = Split-Path -Parent $OutputCsv
if (-not [string]::IsNullOrWhiteSpace($outputDir) -and -not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$seedRows | Export-Csv -Path $OutputCsv -NoTypeInformation -Encoding UTF8

[pscustomobject]@{
    Mode           = "LiveImport"
    InputCsv       = $InputCsv
    OutputCsv      = $OutputCsv
    TotalRows      = $rows.Count
    EnabledRows    = $enabledRows.Count
    ExportedRows   = $seedRows.Count
    UniqueBarcodes = $seenBarcodes.Count
} | ConvertTo-Json
