FROM php:8.3-fpm-alpine

# install composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Install pdo_mysql and bcmath (highly recommended for RabbitMQ libraries)
RUN docker-php-ext-install pdo_mysql bcmath

WORKDIR /var/www/html
COPY htdocs/composer.json /var/www/html/composer.json
RUN composer install --no-dev --prefer-dist --no-interaction --optimize-autoloader

RUN mkdir -p /var/www/data \
	&& chown www-data:www-data /var/www/data \
	&& chmod 700 /var/www/data
