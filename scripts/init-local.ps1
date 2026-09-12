$ErrorActionPreference = 'Stop'
$repoPath = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoPath '.env'
if (Test-Path -LiteralPath $envPath) { throw '.env already exists; edit it rather than replacing credentials.' }
function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$databaseSecret = New-RandomSecret
$redisSecret = New-RandomSecret
$jwtSecret = New-RandomSecret
$adminSecret = 'Aa1!' + (New-RandomSecret)
$contents = @"
DATABASE_PASSWORD=$databaseSecret
REDIS_PASSWORD=$redisSecret
JWT_SECRET=$jwtSecret
CORS_ORIGINS=http://localhost:5173
BOOTSTRAP_ADMIN_EMAIL=admin@example.test
BOOTSTRAP_ADMIN_PASSWORD=$adminSecret
DATABASE_URL=jdbc:postgresql://localhost:5432/real_estate
DATABASE_USER=real_estate
REDIS_HOST=localhost
REDIS_PORT=6379
S3_BUCKET=real-estate
S3_ENDPOINT=http://localhost:9090
S3_PUBLIC_ENDPOINT=http://localhost:9090
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=local-development
AWS_SECRET_ACCESS_KEY=local-development
"@
[IO.File]::WriteAllText($envPath, $contents, [Text.UTF8Encoding]::new($false))
Write-Host 'Created .env with random local credentials. The admin password is in .env.'
Write-Host 'Next: docker compose up --build -d'

