@echo off
set SPRING_DATASOURCE_PASSWORD=W2yY021nH3r3

cd product-service
start /B java -jar target\product-service-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:mysql://localhost:3307/product_db?useSSL=false --spring.datasource.username=root --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672 --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest > ..\product.log 2>&1
cd ..

cd customer-service
start /B java -jar target\customer-service-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:mysql://localhost:3307/customer_db?useSSL=false --spring.datasource.username=root --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672 --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest > ..\customer.log 2>&1
cd ..

cd inventory-service
start /B java -jar target\inventory-service-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:mysql://localhost:3307/inventory_db?useSSL=false --spring.datasource.username=root --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672 --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest > ..\inventory.log 2>&1
cd ..

cd shipping-service
start /B java -jar target\shipping-service-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:mysql://localhost:3307/shipping_db?useSSL=false --spring.datasource.username=root --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672 --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest > ..\shipping.log 2>&1
cd ..

timeout /t 5

cd order-service
start /B java -jar target\order-service-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:mysql://localhost:3307/order_db?useSSL=false --spring.datasource.username=root --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672 --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest > ..\order.log 2>&1
cd ..
