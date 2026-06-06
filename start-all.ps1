$services = @(
    @{name="product-service"; db="product_db"},
    @{name="customer-service"; db="customer_db"},
    @{name="inventory-service"; db="inventory_db"},
    @{name="shipping-service"; db="shipping_db"}
)

foreach ($svc in $services) {
    $args = "-jar target/$($svc.name)-0.0.1-SNAPSHOT.jar --spring.datasource.url=`"jdbc:mysql://localhost:3307/$($svc.db)?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`" --spring.datasource.password=`"W2yY021nH3r3`" --spring.datasource.username=`"root`" --spring.rabbitmq.host=`"localhost`" --spring.rabbitmq.port=5672 --spring.rabbitmq.username=`"guest`" --spring.rabbitmq.password=`"guest`""
    Start-Process -FilePath "java" -ArgumentList $args -WorkingDirectory "d:\API_EAI\$($svc.name)" -RedirectStandardOutput "..\$($svc.name).log" -RedirectStandardError "..\$($svc.name).err"
}

Start-Sleep -Seconds 5

$orderSvc = @{name="order-service"; db="order_db"}
$args = "-jar target/$($orderSvc.name)-0.0.1-SNAPSHOT.jar --spring.datasource.url=`"jdbc:mysql://localhost:3307/$($orderSvc.db)?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`" --spring.datasource.password=`"W2yY021nH3r3`" --spring.datasource.username=`"root`" --spring.rabbitmq.host=`"localhost`" --spring.rabbitmq.port=5672 --spring.rabbitmq.username=`"guest`" --spring.rabbitmq.password=`"guest`""
Start-Process -FilePath "java" -ArgumentList $args -WorkingDirectory "d:\API_EAI\$($orderSvc.name)" -RedirectStandardOutput "..\$($orderSvc.name).log" -RedirectStandardError "..\$($orderSvc.name).err"
