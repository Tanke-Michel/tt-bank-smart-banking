# ==============================================================
# TT-BANK — Fix All Bugs + Run All Tests
# Windows PowerShell — run from inside TT-BANK\Smart-Banking-System\
#
# cd "G:\COURSE-LESSONS\FallLV32025ICT-U\SpringLV3\Software Architecture\TT-BANK\Smart-Banking-System"
# .\fix-and-test.ps1
# ==============================================================

$ErrorActionPreference = "Stop"
$ROOT = Get-Location

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  TT-BANK — Fixing all bugs before testing" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "Working directory: $ROOT"

# ================================================================
# FIX 1: Move test properties to src/test/resources/ with correct name
# They currently exist in src/test/java/.../ with wrong names.
# Spring ONLY reads src/test/resources/application-test.properties
# ================================================================

Write-Host ""
Write-Host "FIX 1: Moving test properties to src/test/resources/ ..." -ForegroundColor Yellow

$testPropFixes = @{
    "services\auth-service\src\test\java\com\example\auth_service\auth-service-application-test.properties"               = "services\auth-service\src\test\resources\application-test.properties"
    "services\wallet-service\src\test\java\com\example\wallet_service\wallet-service-application-test.properties"         = "services\wallet-service\src\test\resources\application-test.properties"
    "services\transaction-service\src\test\java\com\example\transaction_service\transaction-service-application-test.properties" = "services\transaction-service\src\test\resources\application-test.properties"
    "services\merchant-service\src\test\java\com\example\merchant_service\merchant-service-application-test.properties"   = "services\merchant-service\src\test\resources\application-test.properties"
    "services\notification-service\src\test\java\com\example\notification_service\notification-service-application-test.properties" = "services\notification-service\src\test\resources\application-test.properties"
    "services\savings-service\src\test\java\com\example\savings_service\savings-service-application-test.properties"     = "services\savings-service\src\test\resources\application-test.properties"
    "services\audit-service\src\test\java\com\example\audit_service\audit-service-application-test.properties"           = "services\audit-service\src\test\resources\application-test.properties"
    "services\api-gateway\src\test\java\com\example\api_gateway\api-gateway-application-test.properties"                 = "services\api-gateway\src\test\resources\application-test.properties"
}

foreach ($src in $testPropFixes.Keys) {
    $dst = $testPropFixes[$src]
    $dstDir = Split-Path $dst -Parent
    if (-not (Test-Path $dstDir)) {
        New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
    }
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dst -Force
        Write-Host "  Copied: $(Split-Path $src -Leaf) -> $dst" -ForegroundColor Green
    } else {
        Write-Host "  NOT FOUND: $src (skipping)" -ForegroundColor Red
    }
}

# ================================================================
# FIX 2: Copy docker profile overrides into each service's src/main/resources/
# They exist in infrastructure/docker/spring-profiles/ but Spring
# needs them in the service's own src/main/resources/ directory.
# ================================================================

Write-Host ""
Write-Host "FIX 2: Copying Docker profile overrides into service src/main/resources/ ..." -ForegroundColor Yellow

$dockerProfileFixes = @{
    "infrastructure\docker\spring-profiles\auth-service-application-docker.properties"        = "services\auth-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\wallet-service-application-docker.properties"       = "services\wallet-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\transaction-service-application-docker.properties"  = "services\transaction-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\merchant-service-application-docker.properties"     = "services\merchant-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\notification-service-application-docker.properties" = "services\notification-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\savings-service-application-docker.properties"      = "services\savings-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\audit-service-application-docker.properties"        = "services\audit-service\src\main\resources\application-docker.properties"
    "infrastructure\docker\spring-profiles\api-gateway-application-docker.yml"                 = "services\api-gateway\src\main\resources\application-docker.yml"
}

foreach ($src in $dockerProfileFixes.Keys) {
    $dst = $dockerProfileFixes[$src]
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dst -Force
        Write-Host "  Copied: $(Split-Path $src -Leaf)" -ForegroundColor Green
    } else {
        Write-Host "  NOT FOUND: $src" -ForegroundColor Red
    }
}

# ================================================================
# FIX 3: Copy mvnw from auth-service into audit-service
# audit-service is missing the Maven wrapper entirely.
# ================================================================

Write-Host ""
Write-Host "FIX 3: Copying Maven wrapper to audit-service ..." -ForegroundColor Yellow

$auditDir = "services\audit-service"
$authDir  = "services\auth-service"

if (Test-Path "$authDir\mvnw.cmd") {
    Copy-Item -Path "$authDir\mvnw.cmd"      -Destination "$auditDir\mvnw.cmd"      -Force
    Copy-Item -Path "$authDir\mvnw"          -Destination "$auditDir\mvnw"          -Force
    Copy-Item -Path "$authDir\.mvn"          -Destination "$auditDir\.mvn"          -Recurse -Force
    Write-Host "  Copied mvnw.cmd, mvnw, .mvn/ to audit-service" -ForegroundColor Green
} else {
    Write-Host "  mvnw.cmd not found in auth-service — check your repo" -ForegroundColor Red
}

# ================================================================
# FIX 4: Create .env file from template (required for Docker Compose)
# ================================================================

Write-Host ""
Write-Host "FIX 4: Creating .env file ..." -ForegroundColor Yellow

$envSrc = "infrastructure\docker\.env.example"
$envDst = "infrastructure\docker\.env"

if (Test-Path $envDst) {
    Write-Host "  .env already exists — skipping (edit it manually if needed)" -ForegroundColor Cyan
} else {
    Copy-Item -Path $envSrc -Destination $envDst -Force
    Write-Host "  Created .env from .env.example" -ForegroundColor Green
    Write-Host "  EDIT THIS FILE: infrastructure\docker\.env" -ForegroundColor Yellow
    Write-Host "  Set: POSTGRES_PASSWORD, REDIS_PASSWORD, RABBITMQ_PASSWORD, JWT_SECRET, SMTP_USERNAME, SMTP_PASSWORD" -ForegroundColor Yellow
}

# ================================================================
# VERIFICATION: Check all fixes applied correctly
# ================================================================

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  Verifying all fixes..." -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

$checks = @{
    "services\auth-service\src\test\resources\application-test.properties"        = "Auth test props"
    "services\wallet-service\src\test\resources\application-test.properties"      = "Wallet test props"
    "services\transaction-service\src\test\resources\application-test.properties" = "Transaction test props"
    "services\merchant-service\src\test\resources\application-test.properties"    = "Merchant test props"
    "services\notification-service\src\test\resources\application-test.properties"= "Notification test props"
    "services\savings-service\src\test\resources\application-test.properties"     = "Savings test props"
    "services\audit-service\src\test\resources\application-test.properties"       = "Audit test props"
    "services\api-gateway\src\test\resources\application-test.properties"         = "Gateway test props"
    "services\auth-service\src\main\resources\application-docker.properties"      = "Auth docker profile"
    "services\wallet-service\src\main\resources\application-docker.properties"    = "Wallet docker profile"
    "services\audit-service\mvnw.cmd"                                              = "Audit mvnw.cmd"
    "infrastructure\docker\.env"                                                   = ".env file"
}

$allOk = $true
foreach ($path in $checks.Keys) {
    $label = $checks[$path]
    if (Test-Path $path) {
        Write-Host "  OK  $label" -ForegroundColor Green
    } else {
        Write-Host "  MISSING: $label ($path)" -ForegroundColor Red
        $allOk = $false
    }
}

if ($allOk) {
    Write-Host ""
    Write-Host "All fixes applied. Ready to run tests." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Some fixes failed. Fix the MISSING items above before testing." -ForegroundColor Red
}

Write-Host ""
Write-Host "Next: run .\run-tests.ps1 to execute all 232 tests" -ForegroundColor Cyan
