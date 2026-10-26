# TV1 - build, test and benchmark Quick Sort (Hoare + median-of-three).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tv1\run_all.ps1
#   powershell -ExecutionPolicy Bypass -File tv1\run_all.ps1 -Include50m
#   powershell -ExecutionPolicy Bypass -File tv1\run_all.ps1 -Seed 12345 -Dataset d2 -Ns 1000000,10000000
#
# Note: benchmark rows are APPENDED to out\quick_sort_results.csv; the file is never
# rewritten, so every run keeps its own run_id for traceability.

param(
    [switch]$Include50m,
    [long]$Seed = 20261016,
    [string]$Dataset = 'd1',
    [string]$Ns = '',
    [string]$Heap = '-Xmx4g',
    [string]$OutDir = ''
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = if ($OutDir -ne '') { $OutDir } else { Join-Path $scriptDir 'out' }
$classes = Join-Path $out 'classes'
$runId = 'tv1-' + (Get-Date -Format 'yyyyMMdd-HHmmss')

New-Item -ItemType Directory -Force -Path $classes | Out-Null

$sources = @(
    (Join-Path $scriptDir 'src/main/java/sorting/QuickSort.java'),
    (Join-Path $scriptDir 'src/main/java/sorting/DataGenerator.java'),
    (Join-Path $scriptDir 'src/main/java/sorting/bench/QuickBench.java'),
    (Join-Path $scriptDir 'src/test/java/sorting/QuickSortTest.java')
)

Write-Host '== build (--release 8) =='
javac --release 8 -Xlint:all -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Write-Host '== correctness suite =='
java -cp $classes sorting.QuickSortTest "--log=$(Join-Path $out 'quick_sort_test_log.txt')"
if ($LASTEXITCODE -ne 0) { throw "test suite failed with exit code $LASTEXITCODE" }

$csv = Join-Path $out 'quick_sort_results.csv'
$common = @(
    ('--dataset=' + $Dataset),
    ('--seed=' + $Seed),
    ('--out=' + $csv),
    ('--run-id=' + $runId)
)
if ($Ns -ne '') { $common += ('--ns=' + $Ns) }
if ($Include50m) { $common += '--include-50m' }

Write-Host '== benchmark: time mode =='
java $Heap -cp $classes sorting.bench.QuickBench --mode=time "--meta=$(Join-Path $out 'quick_sort_metadata.json')" @common
if ($LASTEXITCODE -ne 0) { throw "benchmark (time) failed with exit code $LASTEXITCODE" }

Write-Host '== benchmark: count mode (separate pass) =='
java $Heap -cp $classes sorting.bench.QuickBench --mode=count "--meta=$(Join-Path $out 'quick_sort_metadata_count.json')" @common
if ($LASTEXITCODE -ne 0) { throw "benchmark (count) failed with exit code $LASTEXITCODE" }

Write-Host '== done =='
Write-Host "csv  : $csv"
Write-Host "log  : $(Join-Path $out 'quick_sort_test_log.txt')"
Write-Host "runId: $runId"
