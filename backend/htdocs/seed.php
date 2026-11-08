<?php

declare(strict_types=1);

const SOURCE_URL = 'https://dummyjson.com/products?limit=0';

function envValue(string $key, string $default): string
{
    $value = getenv($key);
    return $value === false || $value === '' ? $default : $value;
}

function jsonValue(mixed $value): ?string
{
    return $value === null ? null : json_encode($value, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
}

function sourceDate(?array $meta, string $key): ?string
{
    if (empty($meta[$key])) {
        return null;
    }
    return date('Y-m-d H:i:s', strtotime((string) $meta[$key]));
}

$host = envValue('DB_HOST', 'mysql');
$port = envValue('DB_PORT', '3306');
$database = envValue('DB_DATABASE', 'app');
$username = envValue('DB_USERNAME', 'app');
$password = envValue('DB_PASSWORD', 'app_secret');
$dsn = "mysql:host={$host};port={$port};dbname={$database};charset=utf8mb4";

try {
    $pdo = null;
    for ($attempt = 1; $attempt <= 30; $attempt++) {
        try {
            $pdo = new PDO($dsn, $username, $password, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]);
            break;
        } catch (PDOException $exception) {
            if ($attempt === 30) {
                throw $exception;
            }
            sleep(1);
        }
    }

    $pdo->exec(<<<'SQL'
        CREATE TABLE IF NOT EXISTS products (
            id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            description TEXT NULL,
            category VARCHAR(100) NULL,
            price DECIMAL(12, 2) NULL,
            discount_percentage DECIMAL(8, 2) NULL,
            rating DECIMAL(4, 2) NULL,
            stock INT NULL,
            tags JSON NULL,
            brand VARCHAR(255) NULL,
            sku VARCHAR(100) NULL,
            weight INT NULL,
            dimensions JSON NULL,
            warranty_information VARCHAR(255) NULL,
            shipping_information VARCHAR(255) NULL,
            availability_status VARCHAR(100) NULL,
            reviews JSON NULL,
            return_policy VARCHAR(255) NULL,
            minimum_order_quantity INT NULL,
            product_meta JSON NULL,
            thumbnail TEXT NULL,
            images JSON NULL,
            source_created_at DATETIME NULL,
            source_updated_at DATETIME NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY products_sku_unique (sku),
            KEY products_category_index (category),
            KEY products_title_index (title)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    SQL);

    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => 60,
            'header' => "Accept: application/json\r\n",
        ],
    ]);
    $payload = @file_get_contents(SOURCE_URL, false, $context);
    if ($payload === false) {
        throw new RuntimeException('Gagal mengambil data dari DummyJSON.');
    }

    $response = json_decode($payload, true, 512, JSON_THROW_ON_ERROR);
    $products = $response['products'] ?? [];
    if (!is_array($products)) {
        throw new RuntimeException('Format response DummyJSON tidak valid.');
    }

    $statement = $pdo->prepare(<<<'SQL'
        INSERT INTO products (
            id, title, description, category, price, discount_percentage, rating, stock,
            tags, brand, sku, weight, dimensions, warranty_information, shipping_information,
            availability_status, reviews, return_policy, minimum_order_quantity, product_meta,
            thumbnail, images, source_created_at, source_updated_at
        ) VALUES (
            :id, :title, :description, :category, :price, :discount_percentage, :rating, :stock,
            :tags, :brand, :sku, :weight, :dimensions, :warranty_information, :shipping_information,
            :availability_status, :reviews, :return_policy, :minimum_order_quantity, :product_meta,
            :thumbnail, :images, :source_created_at, :source_updated_at
        ) AS new
        ON DUPLICATE KEY UPDATE
            title = new.title,
            description = new.description,
            category = new.category,
            price = new.price,
            discount_percentage = new.discount_percentage,
            rating = new.rating,
            stock = new.stock,
            tags = new.tags,
            brand = new.brand,
            sku = new.sku,
            weight = new.weight,
            dimensions = new.dimensions,
            warranty_information = new.warranty_information,
            shipping_information = new.shipping_information,
            availability_status = new.availability_status,
            reviews = new.reviews,
            return_policy = new.return_policy,
            minimum_order_quantity = new.minimum_order_quantity,
            product_meta = new.product_meta,
            thumbnail = new.thumbnail,
            images = new.images,
            source_created_at = new.source_created_at,
            source_updated_at = new.source_updated_at
    SQL);

    $pdo->beginTransaction();
    foreach ($products as $product) {
        $meta = is_array($product['meta'] ?? null) ? $product['meta'] : null;
        $statement->execute([
            ':id' => (int) $product['id'],
            ':title' => trim((string) ($product['title'] ?? 'Untitled product')),
            ':description' => $product['description'] ?? null,
            ':category' => $product['category'] ?? null,
            ':price' => $product['price'] ?? null,
            ':discount_percentage' => $product['discountPercentage'] ?? null,
            ':rating' => $product['rating'] ?? null,
            ':stock' => $product['stock'] ?? null,
            ':tags' => jsonValue($product['tags'] ?? null),
            ':brand' => $product['brand'] ?? null,
            ':sku' => $product['sku'] ?? null,
            ':weight' => $product['weight'] ?? null,
            ':dimensions' => jsonValue($product['dimensions'] ?? null),
            ':warranty_information' => $product['warrantyInformation'] ?? null,
            ':shipping_information' => $product['shippingInformation'] ?? null,
            ':availability_status' => $product['availabilityStatus'] ?? null,
            ':reviews' => jsonValue($product['reviews'] ?? null),
            ':return_policy' => $product['returnPolicy'] ?? null,
            ':minimum_order_quantity' => $product['minimumOrderQuantity'] ?? null,
            ':product_meta' => jsonValue($meta),
            ':thumbnail' => $product['thumbnail'] ?? null,
            ':images' => jsonValue($product['images'] ?? null),
            ':source_created_at' => sourceDate($meta, 'createdAt'),
            ':source_updated_at' => sourceDate($meta, 'updatedAt'),
        ]);
    }
    $pdo->commit();

    echo sprintf("Berhasil memasukkan atau memperbarui %d produk ke MySQL.%s", count($products), PHP_EOL);
} catch (Throwable $exception) {
    if (isset($pdo) && $pdo instanceof PDO && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    fwrite(STDERR, 'Seed gagal: ' . $exception->getMessage() . PHP_EOL);
    exit(1);
}
