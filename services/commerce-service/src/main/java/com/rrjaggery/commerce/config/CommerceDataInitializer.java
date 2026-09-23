package com.rrjaggery.commerce.config;

import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.entity.ProductGrade;
import com.rrjaggery.commerce.repository.CategoryRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CommerceDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CommerceDataInitializer(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            Category blocks = categoryRepository.save(new Category("Traditional Jaggery Blocks", "traditional-blocks", "Pure organic sugarcane solid blocks crafted with heritage clarification", 1));
            Category powder = categoryRepository.save(new Category("Organic Jaggery Powder", "jaggery-powder", "Fine crystal granulated chemical-free unrefined sugar substitute", 2));
            Category cubes = categoryRepository.save(new Category("Artisanal Jaggery Cubes", "jaggery-cubes", "Uniform moulded tea/coffee sweetener cubes for instant dissolving", 3));
            Category liquid = categoryRepository.save(new Category("Pure Liquid Jaggery (JONNA)", "liquid-jaggery", "Rich iron syrup extract ideal for Ayurvedic preparations", 4));

            System.out.println("Seeded Jaggery Categories.");

            // Product 1: 10KG Traditional Block
            Product p1 = new Product();
            p1.setCategoryId(blocks.getId());
            p1.setName("Mandya Organic Traditional Block Jaggery");
            p1.setSku("RR-BLK-10KG");
            p1.setSlug("mandya-organic-traditional-block-jaggery-10kg");
            p1.setDescription("Authentic solid jaggery block produced from high-sucrose Mandya sugarcane. 100% natural, free from chemical clarifiers or bleaching agents.");
            p1.setGrade(ProductGrade.GRADE_A_TRADITIONAL);
            p1.setPackageType(PackageType.BOX);
            p1.setUnitWeightKg(new BigDecimal("10.000"));
            p1.setRetailPrice(new BigDecimal("550.00"));
            p1.setWholesalePrice(new BigDecimal("420.00"));
            p1.setWholesaleMoq(20);
            p1.setImageUrl("https://images.unsplash.com/photo-1599940824399-b87987ceb72a?auto=format&fit=crop&w=600&q=80");
            p1.setFeatured(true);
            p1.setActive(true);
            productRepository.save(p1);

            // Product 2: 1KG Powder Pouch
            Product p2 = new Product();
            p2.setCategoryId(powder.getId());
            p2.setName("Sulphur-Free Granular Jaggery Powder");
            p2.setSku("RR-POW-01KG");
            p2.setSlug("sulphur-free-granular-jaggery-powder-1kg");
            p2.setDescription("Premium quality pulverized jaggery granules, easy to measure for daily cooking, tea, and confectionery. Retains natural minerals.");
            p2.setGrade(ProductGrade.PREMIUM_POWDER);
            p2.setPackageType(PackageType.POUCH);
            p2.setUnitWeightKg(new BigDecimal("1.000"));
            p2.setRetailPrice(new BigDecimal("85.00"));
            p2.setWholesalePrice(new BigDecimal("62.00"));
            p2.setWholesaleMoq(50);
            p2.setImageUrl("https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=600&q=80");
            p2.setFeatured(true);
            p2.setActive(true);
            productRepository.save(p2);

            // Product 3: 500g Cubes Jar
            Product p3 = new Product();
            p3.setCategoryId(cubes.getId());
            p3.setName("Artisanal Sugarcane Moulded Cubes");
            p3.setSku("RR-CUB-500G");
            p3.setSlug("artisanal-sugarcane-moulded-cubes-500g");
            p3.setDescription("Export grade uniform bite-sized cubes, crafted for clean handling and exact portion sweetening for tea, coffee, and desserts.");
            p3.setGrade(ProductGrade.EXPORT_CUBES);
            p3.setPackageType(PackageType.JAR);
            p3.setUnitWeightKg(new BigDecimal("0.500"));
            p3.setRetailPrice(new BigDecimal("65.00"));
            p3.setWholesalePrice(new BigDecimal("48.00"));
            p3.setWholesaleMoq(30);
            p3.setImageUrl("https://images.unsplash.com/photo-1606787366850-de6330128bfc?auto=format&fit=crop&w=600&q=80");
            p3.setFeatured(true);
            p3.setActive(true);
            productRepository.save(p3);

            // Product 4: 5KG Bulk Bucket
            Product p4 = new Product();
            p4.setCategoryId(liquid.getId());
            p4.setName("Pure Ayurvedic Jonnaguda Liquid Jaggery Syrup");
            p4.setSku("RR-LIQ-05KG");
            p4.setSlug("pure-ayurvedic-jonnaguda-liquid-jaggery-syrup-5kg");
            p4.setDescription("Thick concentrated sugarcane nectar rich in iron, zinc, and magnesium. Widely used in traditional recipes and Ayurvedic tonics.");
            p4.setGrade(ProductGrade.ORGANIC_LIQUID);
            p4.setPackageType(PackageType.BUCKET);
            p4.setUnitWeightKg(new BigDecimal("5.000"));
            p4.setRetailPrice(new BigDecimal("380.00"));
            p4.setWholesalePrice(new BigDecimal("290.00"));
            p4.setWholesaleMoq(15);
            p4.setImageUrl("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80");
            p4.setFeatured(false);
            p4.setActive(true);
            productRepository.save(p4);

            System.out.println("Seeded Mandya Jaggery Products.");
        }
    }
}
