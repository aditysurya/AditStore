<?php

declare(strict_types=1);

final class Config
{
    private const GOOGLE2FA_SECRET_LENGTH = 32;
    private static ?PDO $pdo = null;
    private static ?string $google2faSecretValue = null;

    public static function database(): PDO
    {
        if (self::$pdo instanceof PDO) {
            return self::$pdo;
        }

        $host = getenv('DB_HOST') ?: 'mysql';
        $port = getenv('DB_PORT') ?: '3306';
        $name = getenv('DB_DATABASE') ?: 'app';
        $user = getenv('DB_USERNAME') ?: 'app';
        $password = getenv('DB_PASSWORD') ?: 'app_secret';
        $dsn = "mysql:host={$host};port={$port};dbname={$name};charset=utf8mb4";

        try {
            self::$pdo = new PDO($dsn, $user, $password, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]);
        } catch (PDOException $exception) {
            throw new RuntimeException('Database connection failed.', 503, $exception);
        }

        return self::$pdo;
    }

    public static function google2faSecret(): string
    {
        if (self::$google2faSecretValue !== null) {
            return self::$google2faSecretValue;
        }

        $configuredSecret = getenv('GOOGLE2FA_SECRET');
        if ($configuredSecret !== false && strlen(trim($configuredSecret)) >= self::GOOGLE2FA_SECRET_LENGTH && $configuredSecret !== 'replace_with_a_persistent_secret') {
            self::$google2faSecretValue = trim($configuredSecret);
            return self::$google2faSecretValue;
        }

        $secretPath = getenv('GOOGLE2FA_SECRET_FILE') ?: dirname(__DIR__) . '/data/.google2fa_secret';
        $secretDirectory = dirname($secretPath);
        if (!is_dir($secretDirectory) && !@mkdir($secretDirectory, 0700, true) && !is_dir($secretDirectory)) {
            throw new RuntimeException('Unable to create Google2FA secret directory.', 500);
        }
        if (is_file($secretPath)) {
            $storedSecret = trim((string) file_get_contents($secretPath));
            if (strlen($storedSecret) >= self::GOOGLE2FA_SECRET_LENGTH) {
                self::$google2faSecretValue = $storedSecret;
                return self::$google2faSecretValue;
            }
        }

        $google2fa = new \PragmaRX\Google2FA\Google2FA();
        $generatedSecret = $google2fa->generateSecretKey(self::GOOGLE2FA_SECRET_LENGTH);
        if (@file_put_contents($secretPath, $generatedSecret . PHP_EOL, LOCK_EX) === false) {
            throw new RuntimeException('Unable to persist GOOGLE2FA_SECRET.', 500);
        }
        @chmod($secretPath, 0600);

        self::$google2faSecretValue = $generatedSecret;
        return self::$google2faSecretValue;
    }

    public static function google2faAccount(): string
    {
        return getenv('GOOGLE2FA_ACCOUNT') ?: 'admin@localhost';
    }

    public static function google2faIssuer(): string
    {
        return getenv('GOOGLE2FA_ISSUER') ?: 'Products API';
    }
}