-- ============================================================
-- Root Categories
-- Colors are Material Design 700-900 shades, one per domain.
-- colorHex = -16777216 + R*65536 + G*256 + B  (ARGB signed int)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1006, 'Food & Drink', 'Restaurant', -689152, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1007, 'Transport', 'DirectionsCar', -15374912, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1008, 'Shopping', 'ShoppingBag', -8708190, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1009, 'Housing', 'Home', -16750244, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1010, 'Health', 'HealthAndSafety', -3790808, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1011, 'Entertainment', 'Movie', -9623334, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1012, 'Travel', 'Flight', -16615491, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1013, 'Education', 'School', -15064194, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1014, 'Finance', 'AccountBalance', -13407970, 1, NULL, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1015, 'Subscriptions', 'Subscriptions', -13558894, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1016, 'Gifts & Charity', 'CardGiftcard', -7860657, 1, NULL, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1017, 'Business', 'Work', -13154481, 1, NULL, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1018, 'Income', 'Payments', -14983648, 1, NULL, 'need')

-- ============================================================
-- System Categories  (mandatory, reserved IDs — never delete)
-- ============================================================


-- ============================================================
-- Food & Drink — Level 1  (parentId = 1006)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1106, 'Groceries', 'LocalGroceryStore', -689152, 1, 1006, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1107, 'Dining Out', 'LocalDining', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1108, 'Coffee & Snacks', 'Coffee', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1109, 'Bars & Nightlife', 'LocalBar', -689152, 1, 1006, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1110, 'Food Delivery', 'DeliveryDining', -689152, 1, 1006, 'want')

-- Food & Drink — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Supermarket', 'Store', -689152, 1, 1106, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Specialty Food', 'Storefront', -689152, 1, 1106, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Restaurants', 'Restaurant', -689152, 1, 1107, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fast Food', 'Fastfood', -689152, 1, 1107, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Alcohol', 'LocalBar', -689152, 1, 1109, 'want')

-- ============================================================
-- Transport — Level 1  (parentId = 1007)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1115, 'Own Vehicle', 'DirectionsCar', -15374912, 1, 1007, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1116, 'Public Transit', 'DirectionsBus', -15374912, 1, 1007, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1117, 'Ride & Taxi', 'LocalTaxi', -15374912, 1, 1007, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1118, 'Cycling', 'PedalBike', -15374912, 1, 1007, 'want')

-- Transport — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fuel', 'LocalGasStation', -15374912, 1, 1115, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Parking', 'LocalParking', -15374912, 1, 1115, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Maintenance', 'Build', -15374912, 1, 1115, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Wash', 'LocalCarWash', -15374912, 1, 1115, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bus & Metro', 'DirectionsBus', -15374912, 1, 1116, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Train', 'Train', -15374912, 1, 1116, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Rideshare', 'Hail', -15374912, 1, 1117, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Taxi', 'LocalTaxi', -15374912, 1, 1117, 'want')

-- ============================================================
-- Shopping — Level 1  (parentId = 1008)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1125, 'Clothing & Apparel', 'Checkroom', -8708190, 1, 1008, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1126, 'Electronics', 'Devices', -8708190, 1, 1008, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1127, 'Home & Living', 'Chair', -8708190, 1, 1008, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1128, 'Books & Stationery', 'MenuBook', -8708190, 1, 1008, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1129, 'Online Shopping', 'ShoppingCart', -8708190, 1, 1008, 'want')

-- Shopping — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Clothing', 'Checkroom', -8708190, 1, 1125, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Shoes', 'Hiking', -8708190, 1, 1125, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Accessories', 'Diamond', -8708190, 1, 1125, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Phones', 'Smartphone', -8708190, 1, 1126, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Computers', 'LaptopMac', -8708190, 1, 1126, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Furniture', 'Chair', -8708190, 1, 1127, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Appliances', 'Kitchen', -8708190, 1, 1127, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Home & Garden', 'LocalFlorist', -8708190, 1, 1127, 'want')

-- ============================================================
-- Housing — Level 1  (parentId = 1009)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1135, 'Rent', 'Key', -16750244, 1, 1009, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1136, 'Utilities', 'Bolt', -16750244, 1, 1009, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1137, 'Home Services', 'HomeRepairService', -16750244, 1, 1009, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1138, 'Furnishing', 'Chair', -16750244, 1, 1009, 'want')

-- Housing — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Mortgage', 'Home', -16750244, 1, 1135, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Electricity', 'Lightbulb', -16750244, 1, 1136, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Water', 'WaterDrop', -16750244, 1, 1136, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gas', 'LocalFireDepartment', -16750244, 1, 1136, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Internet', 'Wifi', -16750244, 1, 1136, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Phone', 'Phone', -16750244, 1, 1136, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cleaning', 'CleaningServices', -16750244, 1, 1137, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Repairs', 'Build', -16750244, 1, 1137, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Security', 'Security', -16750244, 1, 1137, 'need')

-- ============================================================
-- Health — Level 1  (parentId = 1010)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1145, 'Medical', 'MedicalServices', -3790808, 1, 1010, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1146, 'Pharmacy', 'LocalPharmacy', -3790808, 1, 1010, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1147, 'Fitness', 'FitnessCenter', -3790808, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1148, 'Mental & Wellbeing', 'SelfImprovement', -3790808, 1, 1010, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1149, 'Personal Care', 'Spa', -3790808, 1, 1010, 'want')

-- Health — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Doctor', 'LocalHospital', -3790808, 1, 1145, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Dentist', 'MedicalServices', -3790808, 1, 1145, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hospital', 'LocalHospital', -3790808, 1, 1145, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vision', 'Visibility', -3790808, 1, 1145, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gym', 'FitnessCenter', -3790808, 1, 1147, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Sports', 'Sports', -3790808, 1, 1147, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Fitness Classes', 'DirectionsRun', -3790808, 1, 1147, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Haircut', 'ContentCut', -3790808, 1, 1149, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Beauty', 'Face', -3790808, 1, 1149, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Spa & Massage', 'Spa', -3790808, 1, 1149, 'want')

-- ============================================================
-- Entertainment — Level 1  (parentId = 1011)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1155, 'Movies & Theater', 'Movie', -9623334, 1, 1011, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1156, 'Music', 'MusicNote', -9623334, 1, 1011, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1157, 'Gaming', 'SportsEsports', -9623334, 1, 1011, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1158, 'Events & Shows', 'EventNote', -9623334, 1, 1011, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1159, 'Hobbies', 'Palette', -9623334, 1, 1011, 'want')

-- Entertainment — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cinema', 'Theaters', -9623334, 1, 1155, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Streaming', 'LocalMovies', -9623334, 1, 1155, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Concerts', 'Festival', -9623334, 1, 1156, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Live Shows', 'TheaterComedy', -9623334, 1, 1158, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Sports Events', 'Stadium', -9623334, 1, 1158, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Arts & Crafts', 'Brush', -9623334, 1, 1159, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Photography', 'CameraAlt', -9623334, 1, 1159, 'want')

-- ============================================================
-- Travel — Level 1  (parentId = 1012)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1165, 'Flights', 'Flight', -16615491, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1166, 'Accommodation', 'Hotel', -16615491, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1167, 'Ground Transport', 'DirectionsBus', -16615491, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1168, 'Activities', 'Attractions', -16615491, 1, 1012, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1169, 'Travel Essentials', 'Luggage', -16615491, 1, 1012, 'want')

-- Travel — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hotels', 'Hotel', -16615491, 1, 1166, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Hostels', 'Bed', -16615491, 1, 1166, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vacation Rental', 'Home', -16615491, 1, 1166, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Car Rental', 'DirectionsCar', -16615491, 1, 1167, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Travel Insurance', 'Shield', -16615491, 1, 1169, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Visas & Permits', 'CardMembership', -16615491, 1, 1169, 'want')

-- ============================================================
-- Education — Level 1  (parentId = 1013)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1175, 'Tuition & Fees', 'School', -15064194, 1, 1013, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1176, 'Online Learning', 'OndemandVideo', -15064194, 1, 1013, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1177, 'Books & Supplies', 'AutoStories', -15064194, 1, 1013, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1178, 'Childcare', 'ChildCare', -15064194, 1, 1013, 'need')

-- Education — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('University', 'AccountBalance', -15064194, 1, 1175, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Training', 'Class', -15064194, 1, 1175, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Certifications', 'Verified', -15064194, 1, 1175, 'need')

-- ============================================================
-- Finance — Level 1  (parentId = 1014)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1185, 'Savings', 'Savings', -13407970, 1, 1014, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1186, 'Investments', 'TrendingUp', -13407970, 1, 1014, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1187, 'Insurance', 'Shield', -13407970, 1, 1014, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1188, 'Taxes & Fees', 'Receipt', -13407970, 1, 1014, 'must')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1189, 'Loans & Debt', 'Money', -13407970, 1, 1014, 'must')

-- Finance — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Stocks', 'ShowChart', -13407970, 1, 1186, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Mutual Funds', 'BarChart', -13407970, 1, 1186, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Crypto', 'CurrencyBitcoin', -13407970, 1, 1186, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Real Estate', 'Domain', -13407970, 1, 1186, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bonds & Funds', 'AccountBalance', -13407970, 1, 1186, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Life Insurance', 'Favorite', -13407970, 1, 1187, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Vehicle Insurance', 'DirectionsCar', -13407970, 1, 1187, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Property Insurance', 'Home', -13407970, 1, 1187, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Student Loan', 'School', -13407970, 1, 1189, 'must')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Credit Card Debt', 'CreditCard', -13407970, 1, 1189, 'must')

-- ============================================================
-- Subscriptions — Level 1  (parentId = 1015)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1190, 'Video Streaming', 'LiveTv', -13558894, 1, 1015, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1191, 'Music & Audio', 'MusicNote', -13558894, 1, 1015, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1192, 'Software Subscriptions', 'Laptop', -13558894, 1, 1015, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1193, 'News & Reading', 'Article', -13558894, 1, 1015, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1194, 'Memberships', 'CardMembership', -13558894, 1, 1015, 'want')

-- Subscriptions — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Cloud Storage', 'Cloud', -13558894, 1, 1192, 'want')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gaming Pass', 'Gamepad', -13558894, 1, 1192, 'want')

-- ============================================================
-- Gifts & Charity — Level 1  (parentId = 1016)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1195, 'Gifts', 'CardGiftcard', -7860657, 1, 1016, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1196, 'Donations', 'VolunteerActivism', -7860657, 1, 1016, 'want')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1197, 'Family Support', 'FamilyRestroom', -7860657, 1, 1016, 'want')

-- ============================================================
-- Business — Level 1  (parentId = 1017)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1198, 'Office & Supplies', 'BusinessCenter', -13154481, 1, 1017, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1199, 'Marketing', 'Campaign', -13154481, 1, 1017, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1200, 'Professional Services', 'Gavel', -13154481, 1, 1017, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1201, 'Client Entertainment', 'Groups', -13154481, 1, 1017, 'want')

-- ============================================================
-- Income — Level 1  (parentId = 1018)
-- ============================================================

INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1202, 'Employment', 'Work', -14983648, 1, 1018, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1203, 'Freelance', 'Handshake', -14983648, 1, 1018, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1204, 'Investment Returns', 'TrendingUp', -14983648, 1, 1018, 'need')
INSERT INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (1205, 'Other Income', 'Payments', -14983648, 1, 1018, 'need')

-- Income — Level 2
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Salary', 'Payments', -14983648, 1, 1202, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Bonus', 'Star', -14983648, 1, 1202, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Commission', 'Percent', -14983648, 1, 1202, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Dividends', 'AccountBalance', -14983648, 1, 1204, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Capital Gains', 'TrendingUp', -14983648, 1, 1204, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Rental Income', 'Home', -14983648, 1, 1204, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Refunds & Cashback', 'ReceiptLong', -14983648, 1, 1205, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Tax Refund', 'AccountBalance', -14983648, 1, 1205, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Government Benefits', 'AccountBalance', -14983648, 1, 1205, 'need')
INSERT INTO categories (name, iconName, colorHex, isDefault, parentId, nature) VALUES ('Gifts Received', 'CardGiftcard', -14983648, 1, 1205, 'need')
