<?php

declare(strict_types=1);

final class Product
{
    private function __construct(private array $attributes)
    {
    }

    public static function fromArray(array $input): self
    {
        $jsonFields = ['tags', 'dimensions', 'reviews', 'meta', 'images'];
        $attributes = [
            'id' => isset($input['id']) ? (int) $input['id'] : null,
            'title' => trim((string) ($input['title'] ?? '')),
            'description' => $input['description'] ?? null,
            'category' => $input['category'] ?? null,
            'price' => isset($input['price']) ? (float) $input['price'] : null,
            'discountPercentage' => isset($input['discountPercentage']) ? (float) $input['discountPercentage'] : null,
            'rating' => isset($input['rating']) ? (float) $input['rating'] : null,
            'stock' => isset($input['stock']) ? (int) $input['stock'] : null,
            'tags' => $input['tags'] ?? null,
            'brand' => $input['brand'] ?? null,
            'sku' => $input['sku'] ?? null,
            'weight' => isset($input['weight']) ? (int) $input['weight'] : null,
            'dimensions' => $input['dimensions'] ?? null,
            'warrantyInformation' => $input['warrantyInformation'] ?? null,
            'shippingInformation' => $input['shippingInformation'] ?? null,
            'availabilityStatus' => $input['availabilityStatus'] ?? null,
            'reviews' => $input['reviews'] ?? null,
            'returnPolicy' => $input['returnPolicy'] ?? null,
            'minimumOrderQuantity' => isset($input['minimumOrderQuantity']) ? (int) $input['minimumOrderQuantity'] : null,
            'meta' => $input['meta'] ?? null,
            'thumbnail' => $input['thumbnail'] ?? null,
            'images' => $input['images'] ?? null,
        ];

        foreach ($jsonFields as $field) {
            if (isset($attributes[$field]) && !is_string($attributes[$field])) {
                $attributes[$field] = json_encode($attributes[$field], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
            }
        }

        return new self($attributes);
    }

    public static function fromDatabaseRow(array $row): self
    {
        $jsonFields = [
            'tags' => 'tags',
            'dimensions' => 'dimensions',
            'reviews' => 'reviews',
            'product_meta' => 'meta',
            'images' => 'images',
        ];

        foreach ($jsonFields as $databaseField => $outputField) {
            $value = $row[$databaseField] ?? null;
            $row[$outputField] = $value === null ? null : json_decode($value, true);
            unset($row[$databaseField]);
        }

        return self::fromArray([
            'id' => $row['id'] ?? null,
            'title' => $row['title'] ?? null,
            'description' => $row['description'] ?? null,
            'category' => $row['category'] ?? null,
            'price' => $row['price'] ?? null,
            'discountPercentage' => $row['discount_percentage'] ?? null,
            'rating' => $row['rating'] ?? null,
            'stock' => $row['stock'] ?? null,
            'tags' => $row['tags'] ?? null,
            'brand' => $row['brand'] ?? null,
            'sku' => $row['sku'] ?? null,
            'weight' => $row['weight'] ?? null,
            'dimensions' => $row['dimensions'] ?? null,
            'warrantyInformation' => $row['warranty_information'] ?? null,
            'shippingInformation' => $row['shipping_information'] ?? null,
            'availabilityStatus' => $row['availability_status'] ?? null,
            'reviews' => $row['reviews'] ?? null,
            'returnPolicy' => $row['return_policy'] ?? null,
            'minimumOrderQuantity' => $row['minimum_order_quantity'] ?? null,
            'meta' => $row['meta'] ?? null,
            'thumbnail' => $row['thumbnail'] ?? null,
            'images' => $row['images'] ?? null,
        ]);
    }

    public function toDatabaseParameters(bool $includeId = false): array
    {
        $parameters = [
            ':title' => $this->attributes['title'],
            ':description' => $this->attributes['description'],
            ':category' => $this->attributes['category'],
            ':price' => $this->attributes['price'],
            ':discount_percentage' => $this->attributes['discountPercentage'],
            ':rating' => $this->attributes['rating'],
            ':stock' => $this->attributes['stock'],
            ':tags' => $this->attributes['tags'],
            ':brand' => $this->attributes['brand'],
            ':sku' => $this->attributes['sku'],
            ':weight' => $this->attributes['weight'],
            ':dimensions' => $this->attributes['dimensions'],
            ':warranty_information' => $this->attributes['warrantyInformation'],
            ':shipping_information' => $this->attributes['shippingInformation'],
            ':availability_status' => $this->attributes['availabilityStatus'],
            ':reviews' => $this->attributes['reviews'],
            ':return_policy' => $this->attributes['returnPolicy'],
            ':minimum_order_quantity' => $this->attributes['minimumOrderQuantity'],
            ':product_meta' => $this->attributes['meta'],
            ':thumbnail' => $this->attributes['thumbnail'],
            ':images' => $this->attributes['images'],
            ':source_created_at' => self::sourceDate($this->attributes['meta'], 'createdAt'),
            ':source_updated_at' => self::sourceDate($this->attributes['meta'], 'updatedAt'),
        ];

        if ($includeId) {
            $parameters[':id'] = $this->attributes['id'];
        }

        return $parameters;
    }

    public function toArray(): array
    {
        $attributes = $this->attributes;
        foreach (['tags', 'dimensions', 'reviews', 'meta', 'images'] as $field) {
            if (isset($attributes[$field]) && is_string($attributes[$field])) {
                $attributes[$field] = json_decode($attributes[$field], true);
            }
        }

        return [
            'id' => $attributes['id'] === null ? null : (int) $attributes['id'],
            'title' => $attributes['title'],
            'description' => $attributes['description'],
            'category' => $attributes['category'],
            'price' => $attributes['price'] === null ? null : (float) $attributes['price'],
            'discountPercentage' => $attributes['discountPercentage'] === null ? null : (float) $attributes['discountPercentage'],
            'rating' => $attributes['rating'] === null ? null : (float) $attributes['rating'],
            'stock' => $attributes['stock'] === null ? null : (int) $attributes['stock'],
            'tags' => $attributes['tags'],
            'brand' => $attributes['brand'],
            'sku' => $attributes['sku'],
            'weight' => $attributes['weight'] === null ? null : (int) $attributes['weight'],
            'dimensions' => $attributes['dimensions'],
            'warrantyInformation' => $attributes['warrantyInformation'],
            'shippingInformation' => $attributes['shippingInformation'],
            'availabilityStatus' => $attributes['availabilityStatus'],
            'reviews' => $attributes['reviews'],
            'returnPolicy' => $attributes['returnPolicy'],
            'minimumOrderQuantity' => $attributes['minimumOrderQuantity'] === null ? null : (int) $attributes['minimumOrderQuantity'],
            'meta' => $attributes['meta'],
            'thumbnail' => $attributes['thumbnail'],
            'images' => $attributes['images'],
        ];
    }

    private static function sourceDate(?string $meta, string $key): ?string
    {
        if ($meta === null) {
            return null;
        }
        $decoded = json_decode($meta, true);
        if (!is_array($decoded) || empty($decoded[$key])) {
            return null;
        }
        return date('Y-m-d H:i:s', strtotime((string) $decoded[$key]));
    }
}