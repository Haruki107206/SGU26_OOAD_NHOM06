CREATE DATABASE IF NOT EXISTS sgu_library CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sgu_library;

CREATE TABLE IF NOT EXISTS the_loai (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ten_the_loai VARCHAR(120) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS nha_xuat_ban (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ten_nxb VARCHAR(180) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tac_gia (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ten_tac_gia VARCHAR(180) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ke_sach (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ma_ke VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sach (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ma_sach VARCHAR(40) NOT NULL UNIQUE,
    ten_sach VARCHAR(240) NOT NULL,
    the_loai_id INT NULL,
    nxb_id INT NULL,
    nam_xuat_ban SMALLINT NOT NULL DEFAULT 0,
    ke_sach_id INT NULL,
    trang_thai ENUM('HOAT_DONG','AN') NOT NULL DEFAULT 'HOAT_DONG',
    CONSTRAINT fk_sach_the_loai FOREIGN KEY(the_loai_id) REFERENCES the_loai(id),
    CONSTRAINT fk_sach_nxb FOREIGN KEY(nxb_id) REFERENCES nha_xuat_ban(id),
    CONSTRAINT fk_sach_ke FOREIGN KEY(ke_sach_id) REFERENCES ke_sach(id),
    INDEX ix_sach_ten(ten_sach), INDEX ix_sach_nam(nam_xuat_ban)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sach_tac_gia (
    sach_id INT NOT NULL,
    tac_gia_id INT NOT NULL,
    PRIMARY KEY(sach_id,tac_gia_id),
    CONSTRAINT fk_sta_sach FOREIGN KEY(sach_id) REFERENCES sach(id),
    CONSTRAINT fk_sta_tac_gia FOREIGN KEY(tac_gia_id) REFERENCES tac_gia(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ban_sao_sach (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sach_id INT NOT NULL,
    ma_vach VARCHAR(80) NOT NULL UNIQUE,
    trang_thai ENUM('SAN_SANG','DANG_MUON','HONG','MAT') NOT NULL DEFAULT 'SAN_SANG',
    CONSTRAINT fk_ban_sao_sach FOREIGN KEY(sach_id) REFERENCES sach(id),
    INDEX ix_ban_sao_sach_trang_thai(sach_id,trang_thai)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS doc_gia (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ma_doc_gia VARCHAR(24) NOT NULL UNIQUE,
    ho_ten VARCHAR(120) NOT NULL,
    ma_dinh_danh VARCHAR(40) NOT NULL UNIQUE,
    don_vi VARCHAR(120) NULL,
    email VARCHAR(160) NULL,
    sdt VARCHAR(20) NULL,
    trang_thai ENUM('HOAT_DONG','KHOA') NOT NULL DEFAULT 'HOAT_DONG',
    INDEX ix_doc_gia_ten(ho_ten)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS the_doc_gia (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ma_the VARCHAR(24) NOT NULL UNIQUE,
    doc_gia_id INT NOT NULL,
    ngay_cap DATE NOT NULL,
    ngay_het_han DATE NOT NULL,
    trang_thai ENUM('HIEU_LUC','KHOA','HET_HAN') NOT NULL DEFAULT 'HIEU_LUC',
    CONSTRAINT fk_the_doc_gia_doc_gia FOREIGN KEY(doc_gia_id) REFERENCES doc_gia(id),
    INDEX ix_the_doc_gia_reader(doc_gia_id,id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS phieu_muon (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ma_phieu VARCHAR(24) NOT NULL UNIQUE,
    doc_gia_id INT NOT NULL,
    ngay_muon DATE NOT NULL,
    han_tra DATE NOT NULL,
    trang_thai ENUM('DANG_MUON','DA_TRA') NOT NULL DEFAULT 'DANG_MUON',
    CONSTRAINT fk_phieu_muon_doc_gia FOREIGN KEY(doc_gia_id) REFERENCES doc_gia(id),
    INDEX ix_phieu_muon_reader_status(doc_gia_id,trang_thai)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ct_phieu_muon (
    id INT PRIMARY KEY AUTO_INCREMENT,
    phieu_muon_id INT NOT NULL,
    ban_sao_sach_id INT NOT NULL,
    ngay_tra DATE NULL,
    trang_thai ENUM('DANG_MUON','DA_TRA','HONG','MAT') NOT NULL DEFAULT 'DANG_MUON',
    CONSTRAINT fk_ct_muon_phieu FOREIGN KEY(phieu_muon_id) REFERENCES phieu_muon(id),
    CONSTRAINT fk_ct_muon_ban_sao FOREIGN KEY(ban_sao_sach_id) REFERENCES ban_sao_sach(id),
    CONSTRAINT uq_ct_muon_phieu_ban_sao UNIQUE(phieu_muon_id,ban_sao_sach_id),
    INDEX ix_ct_muon_status(phieu_muon_id,trang_thai),
    INDEX ix_ct_muon_copy(ban_sao_sach_id,trang_thai)
) ENGINE=InnoDB;

INSERT IGNORE INTO the_loai(ten_the_loai) VALUES ('Công nghệ thông tin'),('Văn học'),('Khoa học');
INSERT IGNORE INTO nha_xuat_ban(ten_nxb) VALUES ('Nhà xuất bản Trẻ'),('Nhà xuất bản Giáo dục Việt Nam');
INSERT IGNORE INTO tac_gia(ten_tac_gia) VALUES ('Nguyễn Nhật Ánh'),('Bộ Giáo dục và Đào tạo');
INSERT IGNORE INTO ke_sach(ma_ke) VALUES ('A-01'),('A-02'),('B-01');
INSERT IGNORE INTO sach(ma_sach,ten_sach,the_loai_id,nxb_id,nam_xuat_ban,ke_sach_id) VALUES
('LIB-001','Cho tôi xin một vé đi tuổi thơ',(SELECT id FROM the_loai WHERE ten_the_loai='Văn học'),(SELECT id FROM nha_xuat_ban WHERE ten_nxb='Nhà xuất bản Trẻ'),2008,(SELECT id FROM ke_sach WHERE ma_ke='A-01')),
('LIB-002','Nhập môn lập trình',(SELECT id FROM the_loai WHERE ten_the_loai='Công nghệ thông tin'),(SELECT id FROM nha_xuat_ban WHERE ten_nxb='Nhà xuất bản Giáo dục Việt Nam'),2022,(SELECT id FROM ke_sach WHERE ma_ke='B-01'));
INSERT IGNORE INTO sach_tac_gia(sach_id,tac_gia_id) SELECT s.id,t.id FROM sach s JOIN tac_gia t ON (s.ma_sach='LIB-001' AND t.ten_tac_gia='Nguyễn Nhật Ánh') OR (s.ma_sach='LIB-002' AND t.ten_tac_gia='Bộ Giáo dục và Đào tạo');
INSERT IGNORE INTO ban_sao_sach(sach_id,ma_vach,trang_thai) SELECT id,'LIB-001-001','SAN_SANG' FROM sach WHERE ma_sach='LIB-001';
INSERT IGNORE INTO ban_sao_sach(sach_id,ma_vach,trang_thai) SELECT id,'LIB-001-002','DANG_MUON' FROM sach WHERE ma_sach='LIB-001';
INSERT IGNORE INTO ban_sao_sach(sach_id,ma_vach,trang_thai) SELECT id,'LIB-002-001','SAN_SANG' FROM sach WHERE ma_sach='LIB-002';

INSERT IGNORE INTO doc_gia(ma_doc_gia,ho_ten,ma_dinh_danh,don_vi,email,sdt) VALUES
('DG-DEMO01','Nguyễn Minh An','SV-DEMO-001','Công nghệ thông tin','minhan@example.edu.vn','0900000001'),
('DG-DEMO02','Trần Thu Hà','SV-DEMO-002','Ngôn ngữ Anh','thuha@example.edu.vn','0900000002');
INSERT IGNORE INTO the_doc_gia(ma_the,doc_gia_id,ngay_cap,ngay_het_han,trang_thai)
SELECT 'THE-DEMO01',id,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 12 MONTH),'HIEU_LUC' FROM doc_gia WHERE ma_doc_gia='DG-DEMO01';
INSERT IGNORE INTO the_doc_gia(ma_the,doc_gia_id,ngay_cap,ngay_het_han,trang_thai)
SELECT 'THE-DEMO02',id,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 12 MONTH),'HIEU_LUC' FROM doc_gia WHERE ma_doc_gia='DG-DEMO02';
INSERT IGNORE INTO phieu_muon(ma_phieu,doc_gia_id,ngay_muon,han_tra,trang_thai)
SELECT 'PM-DEMO01',id,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 14 DAY),'DANG_MUON' FROM doc_gia WHERE ma_doc_gia='DG-DEMO01';
INSERT IGNORE INTO ct_phieu_muon(phieu_muon_id,ban_sao_sach_id,trang_thai)
SELECT pm.id,bs.id,'DANG_MUON' FROM phieu_muon pm JOIN ban_sao_sach bs ON bs.ma_vach='LIB-001-002' WHERE pm.ma_phieu='PM-DEMO01';
