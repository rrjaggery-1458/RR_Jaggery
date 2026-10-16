DO $$
DECLARE
  v_blocks_id uuid;
  v_powder_id uuid;
  v_cubes_id uuid;
  v_liquid_id uuid;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM commerce_schema.categories WHERE slug = 'traditional-blocks') THEN
    INSERT INTO commerce_schema.categories (id, name, slug, description, display_order, active)
    VALUES (gen_random_uuid(), 'Traditional Jaggery Blocks', 'traditional-blocks', 'Pure organic sugarcane solid blocks crafted with heritage clarification', 1, true)
    RETURNING id INTO v_blocks_id;
  ELSE
    SELECT id INTO v_blocks_id FROM commerce_schema.categories WHERE slug = 'traditional-blocks';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.categories WHERE slug = 'jaggery-powder') THEN
    INSERT INTO commerce_schema.categories (id, name, slug, description, display_order, active)
    VALUES (gen_random_uuid(), 'Organic Jaggery Powder', 'jaggery-powder', 'Fine crystal granulated chemical-free unrefined sugar substitute', 2, true)
    RETURNING id INTO v_powder_id;
  ELSE
    SELECT id INTO v_powder_id FROM commerce_schema.categories WHERE slug = 'jaggery-powder';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.categories WHERE slug = 'jaggery-cubes') THEN
    INSERT INTO commerce_schema.categories (id, name, slug, description, display_order, active)
    VALUES (gen_random_uuid(), 'Artisanal Jaggery Cubes', 'jaggery-cubes', 'Uniform moulded tea/coffee sweetener cubes for instant dissolving', 3, true)
    RETURNING id INTO v_cubes_id;
  ELSE
    SELECT id INTO v_cubes_id FROM commerce_schema.categories WHERE slug = 'jaggery-cubes';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.categories WHERE slug = 'liquid-jaggery') THEN
    INSERT INTO commerce_schema.categories (id, name, slug, description, display_order, active)
    VALUES (gen_random_uuid(), 'Pure Liquid Jaggery (JONNA)', 'liquid-jaggery', 'Rich iron syrup extract ideal for Ayurvedic preparations', 4, true)
    RETURNING id INTO v_liquid_id;
  ELSE
    SELECT id INTO v_liquid_id FROM commerce_schema.categories WHERE slug = 'liquid-jaggery';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.products WHERE sku = 'RR-BLK-10KG') THEN
    INSERT INTO commerce_schema.products (id, category_id, name, sku, slug, description, grade, package_type, unit_weight_kg, retail_price, wholesale_price, wholesale_moq, image_url, featured, active)
    VALUES (gen_random_uuid(), v_blocks_id, 'Traditional Farm Block Jaggery', 'RR-BLK-10KG', 'traditional-farm-block-jaggery-10kg', 'Authentic solid jaggery block produced from high-sucrose quality sugarcane. 100% natural, free from chemical clarifiers or bleaching agents.', 'GRADE_A_TRADITIONAL', 'BOX', 10.000, 550.00, 420.00, 20, 'https://images.unsplash.com/photo-1599940824399-b87987ceb72a?auto=format&fit=crop&w=600&q=80', true, true);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.products WHERE sku = 'RR-POW-01KG') THEN
    INSERT INTO commerce_schema.products (id, category_id, name, sku, slug, description, grade, package_type, unit_weight_kg, retail_price, wholesale_price, wholesale_moq, image_url, featured, active)
    VALUES (gen_random_uuid(), v_powder_id, 'Sulphur-Free Granular Jaggery Powder', 'RR-POW-01KG', 'sulphur-free-granular-jaggery-powder-1kg', 'Premium quality pulverized jaggery granules, easy to measure for daily cooking, tea, and confectionery. Retains natural minerals.', 'PREMIUM_POWDER', 'POUCH', 1.000, 85.00, 62.00, 50, 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=600&q=80', true, true);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.products WHERE sku = 'RR-CUB-500G') THEN
    INSERT INTO commerce_schema.products (id, category_id, name, sku, slug, description, grade, package_type, unit_weight_kg, retail_price, wholesale_price, wholesale_moq, image_url, featured, active)
    VALUES (gen_random_uuid(), v_cubes_id, 'Artisanal Sugarcane Moulded Cubes', 'RR-CUB-500G', 'artisanal-sugarcane-moulded-cubes-500g', 'Export grade uniform bite-sized cubes, crafted for clean handling and exact portion sweetening for tea, coffee, and desserts.', 'EXPORT_CUBES', 'JAR', 0.500, 65.00, 48.00, 30, 'https://images.unsplash.com/photo-1606787366850-de6330128bfc?auto=format&fit=crop&w=600&q=80', true, true);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM commerce_schema.products WHERE sku = 'RR-LIQ-05KG') THEN
    INSERT INTO commerce_schema.products (id, category_id, name, sku, slug, description, grade, package_type, unit_weight_kg, retail_price, wholesale_price, wholesale_moq, image_url, featured, active)
    VALUES (gen_random_uuid(), v_liquid_id, 'Pure Ayurvedic Jonnaguda Liquid Jaggery Syrup', 'RR-LIQ-05KG', 'pure-ayurvedic-jonnaguda-liquid-jaggery-syrup-5kg', 'Thick concentrated sugarcane nectar rich in iron, zinc, and magnesium. Widely used in traditional recipes and Ayurvedic tonics.', 'ORGANIC_LIQUID', 'BUCKET', 5.000, 380.00, 290.00, 15, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80', false, true);
  END IF;
END $$;
