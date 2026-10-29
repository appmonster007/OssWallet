-- ============================================================
-- OssWallet default category taxonomy — v3
-- Designed fresh (not a mechanical fold of BudgetBakers' hierarchy):
-- keeps Health & Wellness standalone, avoids duplicate/near-duplicate
-- categories, uses plain names. Borrows the genuinely good structural
-- ideas (Transport/Vehicle split, Communication & Technology root,
-- Finance/Investments split, domain-specific insurance) without
-- copying BudgetBakers' literal label strings.
-- Reserved ids 1000-1005 (Others/Missing/Unknown/InitialBalance/Transfer)
-- are injected by ensureSystemCategories() in AppDatabase.kt BEFORE this
-- file runs — never redefine them here.
-- ============================================================

-- ============================================================
-- Root Categories (15)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1006, 'Food & Drink', 'Restaurant', -689152, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1007, 'Shopping', 'ShoppingBag', -8708190, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1008, 'Housing', 'Home', -16750244, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1009, 'Transportation', 'DirectionsBus', -15374912, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1010, 'Vehicle', 'DirectionsCar', -6543440, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1011, 'Health & Wellness', 'HealthAndSafety', -3790808, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1012, 'Entertainment & Leisure', 'Movie', -9623334, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1013, 'Travel', 'Flight', -16615491, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1014, 'Education', 'School', -15064194, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1015, 'Communication & Technology', 'Computer', -14776091, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1016, 'Finance & Insurance', 'AccountBalance', -13407970, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1017, 'Investments', 'TrendingUp', -1499549, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1018, 'Gifts & Charity', 'CardGiftcard', -7860657, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1019, 'Business', 'Work', -13154481, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1020, 'Income', 'Payments', -14983648, 1, NULL, 'need')

-- ============================================================
-- Food & Drink (parentId = 1006)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1100, 'Groceries', 'LocalGroceryStore', -689152, 1, 1006, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1101, 'Dining Out', 'LocalDining', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1102, 'Coffee & Snacks', 'Coffee', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1103, 'Bars & Nightlife', 'LocalBar', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1104, 'Food Delivery', 'DeliveryDining', -689152, 1, 1006, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Supermarket', 'Store', -689152, 1, 1100, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Specialty Food', 'Storefront', -689152, 1, 1100, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Restaurants', 'Restaurant', -689152, 1, 1101, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fast Food', 'Fastfood', -689152, 1, 1101, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Alcohol', 'Liquor', -689152, 1, 1103, 'want')

-- ============================================================
-- Shopping (parentId = 1007)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1110, 'Clothing & Apparel', 'Checkroom', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1111, 'Electronics', 'Devices', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1112, 'Home & Living', 'Chair', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1113, 'Books & Stationery', 'MenuBook', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1114, 'Online Shopping', 'ShoppingCart', -8708190, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1115, 'Pets & Animals', 'Pets', -8708190, 1, 1007, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Clothing', 'Checkroom', -8708190, 1, 1110, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Shoes', 'Hiking', -8708190, 1, 1110, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Accessories', 'Diamond', -8708190, 1, 1110, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Phones', 'Smartphone', -8708190, 1, 1111, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Computers', 'LaptopMac', -8708190, 1, 1111, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Furniture', 'Chair', -8708190, 1, 1112, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Appliances', 'Kitchen', -8708190, 1, 1112, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Home & Garden', 'LocalFlorist', -8708190, 1, 1112, 'want')

-- ============================================================
-- Housing (parentId = 1008)
-- "Furnishing" dropped: it duplicated Shopping/Home & Living/Furniture
-- for the same real-world purchase. Internet/Phone moved out to
-- Communication & Technology (first-class subs there, not duplicated
-- here). Childcare folded in (recurring family bill, same bucket as
-- Rent/Utilities). Property Insurance kept here, not in a generic
-- Finance bucket, matching Vehicle/Travel's domain-specific insurance.
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1120, 'Rent', 'Key', -16750244, 1, 1008, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1121, 'Mortgage', 'HomeWork', -16750244, 1, 1008, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1122, 'Utilities', 'Bolt', -16750244, 1, 1008, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1123, 'Home Services', 'HomeRepairService', -16750244, 1, 1008, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1124, 'Childcare', 'FamilyRestroom', -16750244, 1, 1008, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1125, 'Property Insurance', 'Shield', -16750244, 1, 1008, 'must')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Electricity', 'Lightbulb', -16750244, 1, 1122, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Water', 'WaterDrop', -16750244, 1, 1122, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gas', 'LocalFireDepartment', -16750244, 1, 1122, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cleaning', 'CleaningServices', -16750244, 1, 1123, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Repairs', 'Build', -16750244, 1, 1123, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Security', 'Security', -16750244, 1, 1123, 'need')

-- ============================================================
-- Transportation (parentId = 1009) — daily commute, public/active
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1130, 'Public Transit', 'DirectionsBus', -15374912, 1, 1009, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1131, 'Ride & Taxi', 'LocalTaxi', -15374912, 1, 1009, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1132, 'Cycling', 'PedalBike', -15374912, 1, 1009, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bus & Metro', 'DirectionsBusFilled', -15374912, 1, 1130, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Train', 'Train', -15374912, 1, 1130, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Rideshare', 'Hail', -15374912, 1, 1131, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Taxi', 'LocalTaxi', -15374912, 1, 1131, 'want')

-- ============================================================
-- Vehicle (parentId = 1010) — costs of owning a vehicle
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1140, 'Fuel', 'LocalGasStation', -6543440, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1141, 'Parking', 'LocalParking', -6543440, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1142, 'Vehicle Maintenance', 'Build', -6543440, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1143, 'Vehicle Insurance', 'Shield', -6543440, 1, 1010, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1144, 'Rentals & Leasing', 'RequestQuote', -6543440, 1, 1010, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Wash', 'LocalCarWash', -6543440, 1, 1142, 'want')

-- ============================================================
-- Health & Wellness (parentId = 1011) — kept standalone on purpose
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1150, 'Medical Care', 'MedicalServices', -3790808, 1, 1011, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1151, 'Pharmacy', 'LocalPharmacy', -3790808, 1, 1011, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1152, 'Fitness', 'FitnessCenter', -3790808, 1, 1011, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1153, 'Mental & Wellbeing', 'SelfImprovement', -3790808, 1, 1011, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1154, 'Personal Care', 'Spa', -3790808, 1, 1011, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Doctor', 'LocalHospital', -3790808, 1, 1150, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Dentist', 'Healing', -3790808, 1, 1150, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hospital', 'Emergency', -3790808, 1, 1150, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vision', 'Visibility', -3790808, 1, 1150, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Health Insurance', 'Shield', -3790808, 1, 1150, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gym', 'FitnessCenter', -3790808, 1, 1152, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Sports', 'Sports', -3790808, 1, 1152, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fitness Classes', 'DirectionsRun', -3790808, 1, 1152, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Haircut', 'ContentCut', -3790808, 1, 1154, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Beauty', 'Face', -3790808, 1, 1154, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Spa & Massage', 'Spa', -3790808, 1, 1154, 'want')

-- ============================================================
-- Entertainment & Leisure (parentId = 1012)
-- "Video Streaming"/"Music & Audio"/"Memberships" dropped: each was a
-- near-duplicate of a category already here (Streaming, Music, and
-- Memberships' own parent respectively).
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1160, 'Movies & Theater', 'Theaters', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1161, 'Music', 'MusicNote', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1162, 'Gaming', 'SportsEsports', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1163, 'Events & Shows', 'EventNote', -9623334, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1164, 'Hobbies', 'Palette', -9623334, 1, 1012, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cinema', 'LocalMovies', -9623334, 1, 1160, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Streaming', 'LiveTv', -9623334, 1, 1160, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Concerts', 'Festival', -9623334, 1, 1161, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Live Shows', 'TheaterComedy', -9623334, 1, 1163, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Sports Events', 'Stadium', -9623334, 1, 1163, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Arts & Crafts', 'Brush', -9623334, 1, 1164, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Photography', 'CameraAlt', -9623334, 1, 1164, 'want')

-- ============================================================
-- Travel (parentId = 1013)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1170, 'Flights', 'Flight', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1171, 'Accommodation', 'Hotel', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1172, 'Ground Transport', 'DirectionsBus', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1173, 'Activities', 'Attractions', -16615491, 1, 1013, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1174, 'Travel Essentials', 'Luggage', -16615491, 1, 1013, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hotels', 'Hotel', -16615491, 1, 1171, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hostels', 'Bed', -16615491, 1, 1171, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vacation Rental', 'House', -16615491, 1, 1171, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Rental', 'DirectionsCar', -16615491, 1, 1172, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Travel Insurance', 'Shield', -16615491, 1, 1174, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Visas & Permits', 'CardTravel', -16615491, 1, 1174, 'want')

-- ============================================================
-- Education (parentId = 1014)
-- Childcare moved out to Housing (recurring family bill, not learning).
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1180, 'Tuition & Fees', 'School', -15064194, 1, 1014, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1181, 'Online Learning', 'OndemandVideo', -15064194, 1, 1014, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1182, 'Books & Supplies', 'AutoStories', -15064194, 1, 1014, 'need')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('University', 'AccountBalance', -15064194, 1, 1180, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Training', 'Class', -15064194, 1, 1180, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Certifications', 'Verified', -15064194, 1, 1180, 'need')

-- ============================================================
-- Communication & Technology (parentId = 1015)
-- New root; Internet/Phone moved here from Housing (first-class, not
-- duplicated in both places).
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1190, 'Internet', 'Wifi', -14776091, 1, 1015, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1191, 'Phone & Mobile', 'Phone', -14776091, 1, 1015, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1192, 'Software & Subscriptions', 'Laptop', -14776091, 1, 1015, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cloud Storage', 'Cloud', -14776091, 1, 1192, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gaming Pass', 'Gamepad', -14776091, 1, 1192, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('News & Reading', 'Article', -14776091, 1, 1192, 'want')

-- ============================================================
-- Finance & Insurance (parentId = 1016) — bills, not assets
-- Domain-specific insurance (Vehicle/Travel/Property/Health) lives in
-- its own root; only Life Insurance (no better home) is here.
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1200, 'Insurance', 'Shield', -13407970, 1, 1016, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1201, 'Taxes & Fees', 'Receipt', -13407970, 1, 1016, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1202, 'Loans & Debt', 'Money', -13407970, 1, 1016, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1203, 'Bank Fees & Fines', 'CurrencyExchange', -13407970, 1, 1016, 'must')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Life Insurance', 'Shield', -13407970, 1, 1200, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Student Loan', 'School', -13407970, 1, 1202, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Credit Card Debt', 'CreditCard', -13407970, 1, 1202, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fines', 'Gavel', -13407970, 1, 1203, 'must')

-- ============================================================
-- Investments (parentId = 1017) — assets, not bills
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1210, 'Savings', 'Savings', -1499549, 1, 1017, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1211, 'Stocks & Funds', 'ShowChart', -1499549, 1, 1017, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1212, 'Real Estate', 'Domain', -1499549, 1, 1017, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1213, 'Crypto', 'CurrencyBitcoin', -1499549, 1, 1017, 'want')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Retirement', 'Savings', -1499549, 1, 1210, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Mutual Funds', 'BarChart', -1499549, 1, 1211, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bonds & Funds', 'AccountBalance', -1499549, 1, 1211, 'want')

-- ============================================================
-- Gifts & Charity (parentId = 1018)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1220, 'Gifts', 'CardGiftcard', -7860657, 1, 1018, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1221, 'Donations', 'VolunteerActivism', -7860657, 1, 1018, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1222, 'Family Support', 'FamilyRestroom', -7860657, 1, 1018, 'want')

-- ============================================================
-- Business (parentId = 1019) — preserved verbatim, no Wallet equivalent
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1230, 'Office & Supplies', 'BusinessCenter', -13154481, 1, 1019, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1231, 'Marketing', 'Campaign', -13154481, 1, 1019, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1232, 'Professional Services', 'Gavel', -13154481, 1, 1019, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1233, 'Client Entertainment', 'Groups', -13154481, 1, 1019, 'want')

-- ============================================================
-- Income (parentId = 1020)
-- ============================================================
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1240, 'Employment', 'Work', -14983648, 1, 1020, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1241, 'Freelance', 'Handshake', -14983648, 1, 1020, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1242, 'Investment Returns', 'TrendingUp', -14983648, 1, 1020, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1243, 'Other Income', 'Payments', -14983648, 1, 1020, 'need')

INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Salary', 'Payments', -14983648, 1, 1240, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bonus', 'Star', -14983648, 1, 1240, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Commission', 'Percent', -14983648, 1, 1240, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Dividends', 'AccountBalance', -14983648, 1, 1242, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Capital Gains', 'TrendingUp', -14983648, 1, 1242, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Rental Income', 'Home', -14983648, 1, 1242, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Business Income', 'BusinessCenter', -14983648, 1, 1243, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Refunds & Cashback', 'ReceiptLong', -14983648, 1, 1243, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Tax Refund', 'AccountBalance', -14983648, 1, 1243, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Government Benefits', 'AccountBalance', -14983648, 1, 1243, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gifts Received', 'CardGiftcard', -14983648, 1, 1243, 'need')
