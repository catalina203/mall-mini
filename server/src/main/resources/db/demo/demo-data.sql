-- 开发环境演示数据（仅 dev profile 且分类表为空时执行）
-- 所有商品均为样例，不包含真实销量、评价或折扣。

INSERT INTO category (id, parent_id, name, icon_url, sort_order, status) VALUES
    (1, 0, '数码', '', 1, 'ENABLED'),
    (2, 0, '服饰', '', 2, 'ENABLED'),
    (3, 0, '家居', '', 3, 'ENABLED'),
    (4, 0, '美妆', '', 4, 'ENABLED'),
    (5, 0, '食品', '', 5, 'ENABLED'),
    (6, 0, '运动', '', 6, 'ENABLED'),
    (11, 1, '手机', '', 1, 'ENABLED'),
    (12, 1, '耳机', '', 2, 'ENABLED'),
    (13, 1, '数码配件', '', 3, 'ENABLED'),
    (21, 2, '男装', '', 1, 'ENABLED'),
    (22, 2, '女装', '', 2, 'ENABLED');

INSERT INTO product (id, category_id, name, subtitle, description, main_image, status, sort_order) VALUES
    (1, 12, '样例·轻量蓝牙耳机', '半入耳设计，日常通勤样例商品', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 100),
    (2, 13, '样例·便携充电宝', '10000mAh / 20000mAh 两种规格', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 95),
    (3, 21, '样例·纯棉基础T恤', '基础版型，多色可选', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 90),
    (4, 22, '样例·轻薄羽绒外套', '轻量保暖，样例服饰', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 85),
    (5, 3, '样例·陶瓷马克杯', '350ml 日常水杯', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 80),
    (6, 3, '样例·香薰蜡烛', '100g 室内香薰', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 75),
    (7, 4, '样例·保湿面霜', '50ml 保湿样例', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 70),
    (8, 4, '样例·氨基酸洁面', '120g 温和清洁', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 65),
    (9, 5, '样例·每日坚果礼盒', '混合坚果组合装', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 60),
    (10, 5, '样例·挂耳咖啡', '10 片装样例咖啡', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 55),
    (11, 6, '样例·瑜伽垫', '183 × 61cm 防滑垫', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 50),
    (12, 6, '样例·运动水壶', '600ml 便携水壶', '这是用于开发演示的样例商品，价格与库存均为测试数据。', 'http://localhost:8080/demo/product.svg', 'ON_SALE', 45),
    (13, 12, '样例·已下架蓝牙耳机', '用于验证下架过滤', '该商品处于下架状态，用户端不应展示。', 'http://localhost:8080/demo/product.svg', 'OFF_SALE', 40);

INSERT INTO product_sku (product_id, sku_code, spec_json, price, stock, status) VALUES
    (1, 'SKU-EARPHONE-BLACK', '{"颜色": "黑色"}', 199.00, 50, 'ENABLED'),
    (1, 'SKU-EARPHONE-WHITE', '{"颜色": "白色"}', 219.00, 30, 'ENABLED'),
    (2, 'SKU-POWER-10000', '{"容量": "10000mAh"}', 129.00, 100, 'ENABLED'),
    (2, 'SKU-POWER-20000', '{"容量": "20000mAh"}', 199.00, 60, 'ENABLED'),
    (3, 'SKU-TSHIRT-WHITE-M', '{"颜色": "白色", "尺码": "M"}', 79.00, 40, 'ENABLED'),
    (3, 'SKU-TSHIRT-WHITE-L', '{"颜色": "白色", "尺码": "L"}', 79.00, 35, 'ENABLED'),
    (3, 'SKU-TSHIRT-BLACK-L', '{"颜色": "黑色", "尺码": "L"}', 79.00, 20, 'ENABLED'),
    (4, 'SKU-JACKET-CREAM-M', '{"颜色": "米白", "尺码": "M"}', 1299.00, 12, 'ENABLED'),
    (4, 'SKU-JACKET-BLACK-L', '{"颜色": "黑色", "尺码": "L"}', 1399.00, 8, 'ENABLED'),
    (5, 'SKU-MUG-350', '{"容量": "350ml"}', 39.00, 80, 'ENABLED'),
    (6, 'SKU-CANDLE-100', '{"规格": "100g"}', 59.00, 60, 'ENABLED'),
    (7, 'SKU-CREAM-50', '{"规格": "50ml"}', 129.00, 45, 'ENABLED'),
    (8, 'SKU-CLEANSER-120', '{"规格": "120g"}', 69.00, 70, 'ENABLED'),
    (9, 'SKU-NUTS-BOX', '{"规格": "混合装"}', 99.00, 30, 'ENABLED'),
    (10, 'SKU-COFFEE-10', '{"规格": "10片装"}', 49.00, 90, 'ENABLED'),
    (11, 'SKU-YOGA-MAT', '{"规格": "183×61cm"}', 89.00, 25, 'ENABLED'),
    (12, 'SKU-BOTTLE-600', '{"容量": "600ml"}', 59.00, 75, 'ENABLED'),
    (13, 'SKU-EARPHONE-OFF', '{"颜色": "灰色"}', 159.00, 10, 'DISABLED');

INSERT INTO product_image (product_id, image_url, sort_order) VALUES
    (1, 'http://localhost:8080/demo/product.svg', 0),
    (2, 'http://localhost:8080/demo/product.svg', 0),
    (3, 'http://localhost:8080/demo/product.svg', 0),
    (4, 'http://localhost:8080/demo/product.svg', 0),
    (5, 'http://localhost:8080/demo/product.svg', 0),
    (6, 'http://localhost:8080/demo/product.svg', 0),
    (7, 'http://localhost:8080/demo/product.svg', 0),
    (8, 'http://localhost:8080/demo/product.svg', 0),
    (9, 'http://localhost:8080/demo/product.svg', 0),
    (10, 'http://localhost:8080/demo/product.svg', 0),
    (11, 'http://localhost:8080/demo/product.svg', 0),
    (12, 'http://localhost:8080/demo/product.svg', 0),
    (13, 'http://localhost:8080/demo/product.svg', 0);

INSERT INTO banner (title, image_url, link_type, link_target, sort_order, status) VALUES
    ('样例轮播 · 蓝牙耳机', 'http://localhost:8080/demo/banner.svg', 'PRODUCT', '1', 1, 'ENABLED'),
    ('样例轮播 · 基础T恤', 'http://localhost:8080/demo/banner.svg', 'PRODUCT', '3', 2, 'ENABLED'),
    ('样例轮播 · 数码分类', 'http://localhost:8080/demo/banner.svg', 'CATEGORY', '1', 3, 'ENABLED');
