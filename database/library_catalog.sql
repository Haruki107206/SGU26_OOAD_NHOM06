/* 1. TAO DATABASE NEU CHUA TON TAI */
IF DB_ID(N'sgu_library') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [sgu_library]');
END;
GO

USE [sgu_library];
GO

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
GO

/* 2. TAO CAC BANG DANH MUC */

-- BANG THE LOAI
IF OBJECT_ID(N'dbo.the_loai', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.the_loai (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ten_the_loai NVARCHAR(120) NOT NULL UNIQUE
    );
END;
GO

-- BANG NHA XUAT BAN
IF OBJECT_ID(N'dbo.nha_xuat_ban', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.nha_xuat_ban (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ten_nxb NVARCHAR(180) NOT NULL UNIQUE
    );
END;
GO

-- BANG TAC GIA
IF OBJECT_ID(N'dbo.tac_gia', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tac_gia (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ten_tac_gia NVARCHAR(180) NOT NULL UNIQUE
    );
END;
GO

-- BANG KE SACH
IF OBJECT_ID(N'dbo.ke_sach', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ke_sach (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma_ke VARCHAR(80) NOT NULL UNIQUE
    );
END;
GO

/* 3. BANG SACH */

IF OBJECT_ID(N'dbo.sach', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sach (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma_sach VARCHAR(40) NOT NULL UNIQUE,
        ten_sach NVARCHAR(240) NOT NULL,
        the_loai_id INT NULL,
        nxb_id INT NULL,
        nam_xuat_ban SMALLINT NOT NULL DEFAULT 0,
        ke_sach_id INT NULL,

        trang_thai VARCHAR(20) NOT NULL DEFAULT 'HOAT_DONG',

        CONSTRAINT CK_sach_trang_thai
            CHECK (trang_thai IN ('HOAT_DONG', 'AN')),

        CONSTRAINT FK_sach_the_loai
            FOREIGN KEY (the_loai_id)
            REFERENCES dbo.the_loai(id),

        CONSTRAINT FK_sach_nxb
            FOREIGN KEY (nxb_id)
            REFERENCES dbo.nha_xuat_ban(id),

        CONSTRAINT FK_sach_ke
            FOREIGN KEY (ke_sach_id)
            REFERENCES dbo.ke_sach(id)
    );

    CREATE INDEX IX_sach_ten
        ON dbo.sach(ten_sach);

    CREATE INDEX IX_sach_nam
        ON dbo.sach(nam_xuat_ban);
END;
GO

/* 4. BANG LIEN KET SACH - TAC GIA */

IF OBJECT_ID(N'dbo.sach_tac_gia', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sach_tac_gia (
        sach_id INT NOT NULL,
        tac_gia_id INT NOT NULL,

        CONSTRAINT PK_sach_tac_gia
            PRIMARY KEY (sach_id, tac_gia_id),

        CONSTRAINT FK_sta_sach
            FOREIGN KEY (sach_id)
            REFERENCES dbo.sach(id),

        CONSTRAINT FK_sta_tac_gia
            FOREIGN KEY (tac_gia_id)
            REFERENCES dbo.tac_gia(id)
    );
END;
GO

/* 5. BANG BAN SAO SACH */

IF OBJECT_ID(N'dbo.ban_sao_sach', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ban_sao_sach (
        id INT IDENTITY(1,1) PRIMARY KEY,
        sach_id INT NOT NULL,
        ma_vach VARCHAR(80) NOT NULL UNIQUE,

        trang_thai VARCHAR(20) NOT NULL DEFAULT 'SAN_SANG',

        CONSTRAINT CK_ban_sao_trang_thai
            CHECK (
                trang_thai IN (
                    'SAN_SANG',
                    'DANG_MUON',
                    'HONG',
                    'MAT'
                )
            ),

        CONSTRAINT FK_ban_sao_sach
            FOREIGN KEY (sach_id)
            REFERENCES dbo.sach(id)
    );

    CREATE INDEX IX_ban_sao_sach_trang_thai
        ON dbo.ban_sao_sach(sach_id, trang_thai);
END;
GO

/* 6. BANG DOC GIA*/

IF OBJECT_ID(N'dbo.doc_gia', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.doc_gia (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma_doc_gia VARCHAR(24) NOT NULL UNIQUE,
        ho_ten NVARCHAR(120) NOT NULL,
        ma_dinh_danh VARCHAR(40) NOT NULL UNIQUE,
        don_vi NVARCHAR(120) NULL,
        email VARCHAR(160) NULL,
        sdt VARCHAR(20) NULL,

        trang_thai VARCHAR(20) NOT NULL DEFAULT 'HOAT_DONG',

        CONSTRAINT CK_doc_gia_trang_thai
            CHECK (trang_thai IN ('HOAT_DONG', 'KHOA'))
    );

    CREATE INDEX IX_doc_gia_ten
        ON dbo.doc_gia(ho_ten);
END;
GO

/* 7. BANG THE DOC GIA */

IF OBJECT_ID(N'dbo.the_doc_gia', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.the_doc_gia (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma_the VARCHAR(24) NOT NULL UNIQUE,
        doc_gia_id INT NOT NULL,
        ngay_cap DATE NOT NULL,
        ngay_het_han DATE NOT NULL,

        trang_thai VARCHAR(20) NOT NULL DEFAULT 'HIEU_LUC',

        CONSTRAINT CK_the_doc_gia_trang_thai
            CHECK (
                trang_thai IN (
                    'HIEU_LUC',
                    'KHOA',
                    'HET_HAN'
                )
            ),

        CONSTRAINT FK_the_doc_gia_doc_gia
            FOREIGN KEY (doc_gia_id)
            REFERENCES dbo.doc_gia(id)
    );

    CREATE INDEX IX_the_doc_gia_reader
        ON dbo.the_doc_gia(doc_gia_id, id);
END;
GO

/* 8. BANG PHIEU MUON */

IF OBJECT_ID(N'dbo.phieu_muon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.phieu_muon (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma_phieu VARCHAR(24) NOT NULL UNIQUE,
        doc_gia_id INT NOT NULL,
        ngay_muon DATE NOT NULL,
        han_tra DATE NOT NULL,

        trang_thai VARCHAR(20) NOT NULL DEFAULT 'DANG_MUON',

        CONSTRAINT CK_phieu_muon_trang_thai
            CHECK (trang_thai IN ('DANG_MUON', 'DA_TRA')),

        CONSTRAINT FK_phieu_muon_doc_gia
            FOREIGN KEY (doc_gia_id)
            REFERENCES dbo.doc_gia(id)
    );

    CREATE INDEX IX_phieu_muon_reader_status
        ON dbo.phieu_muon(doc_gia_id, trang_thai);
END;
GO

/* 9. BANG CHI TIET PHIEU MUON */

IF OBJECT_ID(N'dbo.ct_phieu_muon', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ct_phieu_muon (
        id INT IDENTITY(1,1) PRIMARY KEY,
        phieu_muon_id INT NOT NULL,
        ban_sao_sach_id INT NOT NULL,
        ngay_tra DATE NULL,

        trang_thai VARCHAR(20) NOT NULL DEFAULT 'DANG_MUON',

        CONSTRAINT CK_ct_phieu_muon_trang_thai
            CHECK (
                trang_thai IN (
                    'DANG_MUON',
                    'DA_TRA',
                    'HONG',
                    'MAT'
                )
            ),

        CONSTRAINT FK_ct_muon_phieu
            FOREIGN KEY (phieu_muon_id)
            REFERENCES dbo.phieu_muon(id),

        CONSTRAINT FK_ct_muon_ban_sao
            FOREIGN KEY (ban_sao_sach_id)
            REFERENCES dbo.ban_sao_sach(id),

        CONSTRAINT UQ_ct_muon_phieu_ban_sao
            UNIQUE (phieu_muon_id, ban_sao_sach_id)
    );

    CREATE INDEX IX_ct_muon_status
        ON dbo.ct_phieu_muon(phieu_muon_id, trang_thai);

    CREATE INDEX IX_ct_muon_copy
        ON dbo.ct_phieu_muon(ban_sao_sach_id, trang_thai);
END;
GO

/* BO SUNG TAI KHOAN, VAI TRO VA LIEN KET VOI SCHEMA HIEN TAI */

IF OBJECT_ID(N'dbo.vai_tro', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.vai_tro (
        ma_vai_tro INT IDENTITY(1,1) PRIMARY KEY,
        ten_vai_tro NVARCHAR(40) NOT NULL UNIQUE,
        mo_ta NVARCHAR(250) NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.tai_khoan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tai_khoan (
        ma_tai_khoan INT IDENTITY(1,1) PRIMARY KEY,
        ma_vai_tro INT NOT NULL,
        ten_dang_nhap NVARCHAR(50) NOT NULL UNIQUE,
        mat_khau_hash VARCHAR(255) NOT NULL,
        trang_thai VARCHAR(20) NOT NULL DEFAULT 'HOAT_DONG',
        so_lan_sai TINYINT NOT NULL DEFAULT 0,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT CK_tai_khoan_trang_thai
            CHECK (trang_thai IN ('HOAT_DONG', 'KHOA')),
        CONSTRAINT CK_tai_khoan_so_lan_sai
            CHECK (so_lan_sai <= 6),
        CONSTRAINT FK_tai_khoan_vai_tro
            FOREIGN KEY (ma_vai_tro) REFERENCES dbo.vai_tro(ma_vai_tro)
    );
END;
GO

IF COL_LENGTH(N'dbo.doc_gia', N'ngay_sinh') IS NULL
    ALTER TABLE dbo.doc_gia ADD ngay_sinh DATE NULL;
GO

IF COL_LENGTH(N'dbo.doc_gia', N'ma_tai_khoan') IS NULL
    ALTER TABLE dbo.doc_gia ADD ma_tai_khoan INT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = N'FK_doc_gia_tai_khoan'
      AND parent_object_id = OBJECT_ID(N'dbo.doc_gia')
)
    ALTER TABLE dbo.doc_gia
        ADD CONSTRAINT FK_doc_gia_tai_khoan
        FOREIGN KEY (ma_tai_khoan) REFERENCES dbo.tai_khoan(ma_tai_khoan);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UX_doc_gia_ma_tai_khoan'
      AND object_id = OBJECT_ID(N'dbo.doc_gia')
)
    CREATE UNIQUE INDEX UX_doc_gia_ma_tai_khoan
        ON dbo.doc_gia(ma_tai_khoan)
        WHERE ma_tai_khoan IS NOT NULL;
GO

IF COL_LENGTH(N'dbo.the_doc_gia', N'ly_do_khoa') IS NULL
    ALTER TABLE dbo.the_doc_gia ADD ly_do_khoa NVARCHAR(250) NULL;
GO

IF COL_LENGTH(N'dbo.phieu_muon', N'ma_thu_thu') IS NULL
    ALTER TABLE dbo.phieu_muon ADD ma_thu_thu INT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = N'FK_phieu_muon_tai_khoan'
      AND parent_object_id = OBJECT_ID(N'dbo.phieu_muon')
)
    ALTER TABLE dbo.phieu_muon
        ADD CONSTRAINT FK_phieu_muon_tai_khoan
        FOREIGN KEY (ma_thu_thu) REFERENCES dbo.tai_khoan(ma_tai_khoan);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UX_the_doc_gia_mot_the_hieu_luc'
      AND object_id = OBJECT_ID(N'dbo.the_doc_gia')
)
BEGIN
    IF EXISTS (
        SELECT doc_gia_id
        FROM dbo.the_doc_gia
        WHERE trang_thai = 'HIEU_LUC'
        GROUP BY doc_gia_id
        HAVING COUNT(*) > 1
    )
        THROW 51000, N'Can xu ly cac doc gia co nhieu the HIEU_LUC truoc khi tao unique index.', 1;

    CREATE UNIQUE INDEX UX_the_doc_gia_mot_the_hieu_luc
        ON dbo.the_doc_gia(doc_gia_id)
        WHERE trang_thai = 'HIEU_LUC';
END;
GO

/* PHIEU PHAT VA CHI TIET VI PHAM */

IF OBJECT_ID(N'dbo.phieu_phat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.phieu_phat (
        ma_phieu_phat BIGINT IDENTITY(1,1) PRIMARY KEY,
        doc_gia_id INT NOT NULL,
        ma_thu_thu INT NOT NULL,
        ngay_lap DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        tong_tien DECIMAL(12,2) NOT NULL DEFAULT 0,
        trang_thai VARCHAR(20) NOT NULL DEFAULT 'ChuaThanhToan',
        hinh_thuc_tt VARCHAR(20) NULL,
        ngay_thanh_toan DATETIME2 NULL,
        CONSTRAINT CK_phieu_phat_tong_tien CHECK (tong_tien >= 0),
        CONSTRAINT CK_phieu_phat_trang_thai
            CHECK (trang_thai IN ('ChuaThanhToan', 'DaThanhToan')),
        CONSTRAINT CK_phieu_phat_hinh_thuc
            CHECK (hinh_thuc_tt IS NULL OR hinh_thuc_tt IN ('TienMat', 'QR')),
        CONSTRAINT CK_phieu_phat_thanh_toan
            CHECK (
                (trang_thai = 'ChuaThanhToan' AND hinh_thuc_tt IS NULL AND ngay_thanh_toan IS NULL)
                OR
                (trang_thai = 'DaThanhToan' AND hinh_thuc_tt IS NOT NULL AND ngay_thanh_toan IS NOT NULL)
            ),
        CONSTRAINT FK_phieu_phat_doc_gia
            FOREIGN KEY (doc_gia_id) REFERENCES dbo.doc_gia(id),
        CONSTRAINT FK_phieu_phat_tai_khoan
            FOREIGN KEY (ma_thu_thu) REFERENCES dbo.tai_khoan(ma_tai_khoan)
    );
    CREATE INDEX IX_phieu_phat_doc_gia_trang_thai
        ON dbo.phieu_phat(doc_gia_id, trang_thai);
END;
GO

IF OBJECT_ID(N'dbo.ct_phieu_phat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ct_phieu_phat (
        ma_ct_phat BIGINT IDENTITY(1,1) PRIMARY KEY,
        ma_phieu_phat BIGINT NOT NULL,
        ma_ct_muon INT NULL,
        loai_vi_pham VARCHAR(20) NOT NULL,
        so_ngay_tre INT NULL,
        so_tien DECIMAL(12,2) NOT NULL,
        mo_ta NVARCHAR(250) NULL,
        CONSTRAINT CK_ct_phieu_phat_loai
            CHECK (loai_vi_pham IN ('QuaHan', 'Hong', 'Mat', 'Khac')),
        CONSTRAINT CK_ct_phieu_phat_so_ngay CHECK (so_ngay_tre IS NULL OR so_ngay_tre >= 0),
        CONSTRAINT CK_ct_phieu_phat_so_tien CHECK (so_tien >= 0),
        CONSTRAINT FK_ct_phieu_phat_phieu
            FOREIGN KEY (ma_phieu_phat) REFERENCES dbo.phieu_phat(ma_phieu_phat),
        CONSTRAINT FK_ct_phieu_phat_ct_muon
            FOREIGN KEY (ma_ct_muon) REFERENCES dbo.ct_phieu_muon(id)
    );
    CREATE INDEX IX_ct_phieu_phat_phieu
        ON dbo.ct_phieu_phat(ma_phieu_phat);
    CREATE INDEX IX_ct_phieu_phat_ct_muon
        ON dbo.ct_phieu_phat(ma_ct_muon)
        WHERE ma_ct_muon IS NOT NULL;
END;
GO

CREATE OR ALTER TRIGGER dbo.TR_ct_phieu_phat_sync_total
ON dbo.ct_phieu_phat
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH changed AS (
        SELECT ma_phieu_phat FROM inserted
        UNION
        SELECT ma_phieu_phat FROM deleted
    )
    UPDATE pp
    SET tong_tien = COALESCE((
        SELECT SUM(ct.so_tien)
        FROM dbo.ct_phieu_phat ct
        WHERE ct.ma_phieu_phat = pp.ma_phieu_phat
    ), 0)
    FROM dbo.phieu_phat pp
    JOIN changed ON changed.ma_phieu_phat = pp.ma_phieu_phat;
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ten_vai_tro = N'Admin')
    INSERT INTO dbo.vai_tro(ten_vai_tro, mo_ta)
    VALUES (N'Admin', N'Quản trị tài khoản và hệ thống');

IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ten_vai_tro = N'ThuThu')
    INSERT INTO dbo.vai_tro(ten_vai_tro, mo_ta)
    VALUES (N'ThuThu', N'Nghiệp vụ thư viện');

IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ten_vai_tro = N'DocGia')
    INSERT INTO dbo.vai_tro(ten_vai_tro, mo_ta)
    VALUES (N'DocGia', N'Tra cứu và xem hồ sơ cá nhân');
GO

CREATE OR ALTER TRIGGER dbo.TR_tai_khoan_single_admin
ON dbo.tai_khoan
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN dbo.vai_tro r ON r.ma_vai_tro = i.ma_vai_tro
        WHERE r.ten_vai_tro = N'Admin'
    ) OR EXISTS (
        SELECT 1
        FROM deleted d
        JOIN dbo.vai_tro r ON r.ma_vai_tro = d.ma_vai_tro
        WHERE r.ten_vai_tro = N'Admin'
    )
    BEGIN
        DECLARE @adminRoleId INT;
        SELECT @adminRoleId = ma_vai_tro
        FROM dbo.vai_tro WITH (UPDLOCK, HOLDLOCK)
        WHERE ten_vai_tro = N'Admin';

        IF @adminRoleId IS NULL
            RETURN;

        IF EXISTS (
            SELECT 1
            FROM dbo.tai_khoan
            WHERE ma_vai_tro = @adminRoleId
              AND CONVERT(VARBINARY(100), ten_dang_nhap)
                  <> CONVERT(VARBINARY(100), N'Admin')
        )
        BEGIN
            THROW 51020, N'Tài khoản Admin duy nhất phải có tên đăng nhập Admin.', 1;
        END;

        IF (
            SELECT COUNT_BIG(*)
            FROM dbo.tai_khoan WITH (UPDLOCK, HOLDLOCK)
            WHERE ma_vai_tro = @adminRoleId
        ) > 1
        BEGIN
            THROW 51021, N'Chỉ được tồn tại một tài khoản Admin.', 1;
        END;
    END;
END;
GO

/* 10. DU LIEU MAU - THE LOAI */

IF NOT EXISTS (
    SELECT 1 FROM dbo.the_loai
    WHERE ten_the_loai = N'Công nghệ thông tin'
)
INSERT INTO dbo.the_loai(ten_the_loai)
VALUES (N'Công nghệ thông tin');

IF NOT EXISTS (
    SELECT 1 FROM dbo.the_loai
    WHERE ten_the_loai = N'Văn học'
)
INSERT INTO dbo.the_loai(ten_the_loai)
VALUES (N'Văn học');

IF NOT EXISTS (
    SELECT 1 FROM dbo.the_loai
    WHERE ten_the_loai = N'Khoa học'
)
INSERT INTO dbo.the_loai(ten_the_loai)
VALUES (N'Khoa học');
GO

/* 11. DU LIEU MAU - NHA XUAT BAN */

IF NOT EXISTS (
    SELECT 1 FROM dbo.nha_xuat_ban
    WHERE ten_nxb = N'Nhà xuất bản Trẻ'
)
INSERT INTO dbo.nha_xuat_ban(ten_nxb)
VALUES (N'Nhà xuất bản Trẻ');

IF NOT EXISTS (
    SELECT 1 FROM dbo.nha_xuat_ban
    WHERE ten_nxb = N'Nhà xuất bản Giáo dục Việt Nam'
)
INSERT INTO dbo.nha_xuat_ban(ten_nxb)
VALUES (N'Nhà xuất bản Giáo dục Việt Nam');
GO

/* 12. DU LIEU MAU - TAC GIA */

IF NOT EXISTS (
    SELECT 1 FROM dbo.tac_gia
    WHERE ten_tac_gia = N'Nguyễn Nhật Ánh'
)
INSERT INTO dbo.tac_gia(ten_tac_gia)
VALUES (N'Nguyễn Nhật Ánh');

IF NOT EXISTS (
    SELECT 1 FROM dbo.tac_gia
    WHERE ten_tac_gia = N'Bộ Giáo dục và Đào tạo'
)
INSERT INTO dbo.tac_gia(ten_tac_gia)
VALUES (N'Bộ Giáo dục và Đào tạo');
GO

/* 13. SUA DU LIEU TIENG VIET BI SAI MA HOA */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

UPDATE s
SET the_loai_id = correct.id
FROM dbo.sach s
JOIN dbo.the_loai broken ON s.the_loai_id = broken.id
JOIN dbo.the_loai correct ON correct.ten_the_loai = N'Công nghệ thông tin'
WHERE CONVERT(VARBINARY(MAX), broken.ten_the_loai) =
      0x4300C300B4006E00670020006E0067006800E100BB002120200074006800C300B4006E0067002000740069006E00;

UPDATE s
SET the_loai_id = correct.id
FROM dbo.sach s
JOIN dbo.the_loai broken ON s.the_loai_id = broken.id
JOIN dbo.the_loai correct ON correct.ten_the_loai = N'Văn học'
WHERE CONVERT(VARBINARY(MAX), broken.ten_the_loai) =
      0x5600C40092016E0020006800E100BB008D006300;

UPDATE s
SET the_loai_id = correct.id
FROM dbo.sach s
JOIN dbo.the_loai broken ON s.the_loai_id = broken.id
JOIN dbo.the_loai correct ON correct.ten_the_loai = N'Khoa học'
WHERE CONVERT(VARBINARY(MAX), broken.ten_the_loai) =
      0x4B0068006F00610020006800E100BB008D006300;

DELETE broken
FROM dbo.the_loai broken
WHERE CONVERT(VARBINARY(MAX), broken.ten_the_loai) IN (
    0x4300C300B4006E00670020006E0067006800E100BB002120200074006800C300B4006E0067002000740069006E00,
    0x5600C40092016E0020006800E100BB008D006300,
    0x4B0068006F00610020006800E100BB008D006300
);

UPDATE s
SET nxb_id = correct.id
FROM dbo.sach s
JOIN dbo.nha_xuat_ban broken ON s.nxb_id = broken.id
JOIN dbo.nha_xuat_ban correct ON correct.ten_nxb = N'Nhà xuất bản Trẻ'
WHERE CONVERT(VARBINARY(MAX), broken.ten_nxb) =
      0x4E006800C300A000200078007500E100BA00A500740020006200E100BA00A3006E00200054007200E100BA00BB00;

UPDATE s
SET nxb_id = correct.id
FROM dbo.sach s
JOIN dbo.nha_xuat_ban broken ON s.nxb_id = broken.id
JOIN dbo.nha_xuat_ban correct ON correct.ten_nxb = N'Nhà xuất bản Giáo dục Việt Nam'
WHERE CONVERT(VARBINARY(MAX), broken.ten_nxb) =
      0x4E006800C300A000200078007500E100BA00A500740020006200E100BA00A3006E00200047006900C300A1006F0020006400E100BB00A5006300200056006900E100BB002120740020004E0061006D00;

DELETE broken
FROM dbo.nha_xuat_ban broken
WHERE CONVERT(VARBINARY(MAX), broken.ten_nxb) IN (
    0x4E006800C300A000200078007500E100BA00A500740020006200E100BA00A3006E00200054007200E100BA00BB00,
    0x4E006800C300A000200078007500E100BA00A500740020006200E100BA00A3006E00200047006900C300A1006F0020006400E100BB00A5006300200056006900E100BB002120740020004E0061006D00
);

DELETE broken_link
FROM dbo.sach_tac_gia broken_link
JOIN dbo.tac_gia broken ON broken_link.tac_gia_id = broken.id
JOIN dbo.tac_gia correct ON correct.ten_tac_gia = N'Nguyễn Nhật Ánh'
JOIN dbo.sach_tac_gia correct_link
    ON correct_link.sach_id = broken_link.sach_id
   AND correct_link.tac_gia_id = correct.id
WHERE CONVERT(VARBINARY(MAX), broken.ten_tac_gia) =
      0x4E00670075007900E100BB0026206E0020004E006800E100BA00AD0074002000C30081006E006800;

UPDATE broken_link
SET tac_gia_id = correct.id
FROM dbo.sach_tac_gia broken_link
JOIN dbo.tac_gia broken ON broken_link.tac_gia_id = broken.id
JOIN dbo.tac_gia correct ON correct.ten_tac_gia = N'Nguyễn Nhật Ánh'
WHERE CONVERT(VARBINARY(MAX), broken.ten_tac_gia) =
      0x4E00670075007900E100BB0026206E0020004E006800E100BA00AD0074002000C30081006E006800;

DELETE broken_link
FROM dbo.sach_tac_gia broken_link
JOIN dbo.tac_gia broken ON broken_link.tac_gia_id = broken.id
JOIN dbo.tac_gia correct ON correct.ten_tac_gia = N'Bộ Giáo dục và Đào tạo'
JOIN dbo.sach_tac_gia correct_link
    ON correct_link.sach_id = broken_link.sach_id
   AND correct_link.tac_gia_id = correct.id
WHERE CONVERT(VARBINARY(MAX), broken.ten_tac_gia) =
      0x4200E100BB002221200047006900C300A1006F0020006400E100BB00A500630020007600C300A0002000C4009000C300A0006F0020007400E100BA00A1006F00;

UPDATE broken_link
SET tac_gia_id = correct.id
FROM dbo.sach_tac_gia broken_link
JOIN dbo.tac_gia broken ON broken_link.tac_gia_id = broken.id
JOIN dbo.tac_gia correct ON correct.ten_tac_gia = N'Bộ Giáo dục và Đào tạo'
WHERE CONVERT(VARBINARY(MAX), broken.ten_tac_gia) =
      0x4200E100BB002221200047006900C300A1006F0020006400E100BB00A500630020007600C300A0002000C4009000C300A0006F0020007400E100BA00A1006F00;

DELETE broken
FROM dbo.tac_gia broken
WHERE CONVERT(VARBINARY(MAX), broken.ten_tac_gia) IN (
    0x4E00670075007900E100BB0026206E0020004E006800E100BA00AD0074002000C30081006E006800,
    0x4200E100BB002221200047006900C300A1006F0020006400E100BB00A500630020007600C300A0002000C4009000C300A0006F0020007400E100BA00A1006F00
);

COMMIT TRANSACTION;
GO

/* 14. DU LIEU MAU - KE SACH */

IF NOT EXISTS (SELECT 1 FROM dbo.ke_sach WHERE ma_ke = 'A-01')
INSERT INTO dbo.ke_sach(ma_ke) VALUES ('A-01');

IF NOT EXISTS (SELECT 1 FROM dbo.ke_sach WHERE ma_ke = 'A-02')
INSERT INTO dbo.ke_sach(ma_ke) VALUES ('A-02');

IF NOT EXISTS (SELECT 1 FROM dbo.ke_sach WHERE ma_ke = 'B-01')
INSERT INTO dbo.ke_sach(ma_ke) VALUES ('B-01');
GO

/* 15. DU LIEU MAU - SACH */

IF NOT EXISTS (SELECT 1 FROM dbo.sach WHERE ma_sach = 'LIB-001')
BEGIN
    INSERT INTO dbo.sach(
        ma_sach,
        ten_sach,
        the_loai_id,
        nxb_id,
        nam_xuat_ban,
        ke_sach_id
    )
    SELECT
        'LIB-001',
        N'Cho tôi xin một vé đi tuổi thơ',
        tl.id,
        nxb.id,
        2008,
        k.id
    FROM dbo.the_loai tl
    CROSS JOIN dbo.nha_xuat_ban nxb
    CROSS JOIN dbo.ke_sach k
    WHERE tl.ten_the_loai = N'Văn học'
      AND nxb.ten_nxb = N'Nhà xuất bản Trẻ'
      AND k.ma_ke = 'A-01';
END;

IF NOT EXISTS (SELECT 1 FROM dbo.sach WHERE ma_sach = 'LIB-002')
BEGIN
    INSERT INTO dbo.sach(
        ma_sach,
        ten_sach,
        the_loai_id,
        nxb_id,
        nam_xuat_ban,
        ke_sach_id
    )
    SELECT
        'LIB-002',
        N'Nhập môn lập trình',
        tl.id,
        nxb.id,
        2022,
        k.id
    FROM dbo.the_loai tl
    CROSS JOIN dbo.nha_xuat_ban nxb
    CROSS JOIN dbo.ke_sach k
    WHERE tl.ten_the_loai = N'Công nghệ thông tin'
      AND nxb.ten_nxb = N'Nhà xuất bản Giáo dục Việt Nam'
      AND k.ma_ke = 'B-01';
END;
GO

/* 16. LIEN KET SACH - TAC GIA */

IF NOT EXISTS (
    SELECT 1
    FROM dbo.sach_tac_gia st
    JOIN dbo.sach s ON st.sach_id = s.id
    JOIN dbo.tac_gia tg ON st.tac_gia_id = tg.id
    WHERE s.ma_sach = 'LIB-001'
      AND tg.ten_tac_gia = N'Nguyễn Nhật Ánh'
)
BEGIN
    INSERT INTO dbo.sach_tac_gia(sach_id, tac_gia_id)
    SELECT s.id, tg.id
    FROM dbo.sach s
    CROSS JOIN dbo.tac_gia tg
    WHERE s.ma_sach = 'LIB-001'
      AND tg.ten_tac_gia = N'Nguyễn Nhật Ánh';
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.sach_tac_gia st
    JOIN dbo.sach s ON st.sach_id = s.id
    JOIN dbo.tac_gia tg ON st.tac_gia_id = tg.id
    WHERE s.ma_sach = 'LIB-002'
      AND tg.ten_tac_gia = N'Bộ Giáo dục và Đào tạo'
)
BEGIN
    INSERT INTO dbo.sach_tac_gia(sach_id, tac_gia_id)
    SELECT s.id, tg.id
    FROM dbo.sach s
    CROSS JOIN dbo.tac_gia tg
    WHERE s.ma_sach = 'LIB-002'
      AND tg.ten_tac_gia = N'Bộ Giáo dục và Đào tạo';
END;
GO

/* 17. DU LIEU MAU - BAN SAO SACH */

IF NOT EXISTS (
    SELECT 1 FROM dbo.ban_sao_sach
    WHERE ma_vach = 'LIB-001-001'
)
INSERT INTO dbo.ban_sao_sach(sach_id, ma_vach, trang_thai)
SELECT id, 'LIB-001-001', 'SAN_SANG'
FROM dbo.sach
WHERE ma_sach = 'LIB-001';

IF NOT EXISTS (
    SELECT 1 FROM dbo.ban_sao_sach
    WHERE ma_vach = 'LIB-001-002'
)
INSERT INTO dbo.ban_sao_sach(sach_id, ma_vach, trang_thai)
SELECT id, 'LIB-001-002', 'DANG_MUON'
FROM dbo.sach
WHERE ma_sach = 'LIB-001';

IF NOT EXISTS (
    SELECT 1 FROM dbo.ban_sao_sach
    WHERE ma_vach = 'LIB-002-001'
)
INSERT INTO dbo.ban_sao_sach(sach_id, ma_vach, trang_thai)
SELECT id, 'LIB-002-001', 'SAN_SANG'
FROM dbo.sach
WHERE ma_sach = 'LIB-002';
GO

/* 18. DU LIEU MAU - DOC GIA */

IF NOT EXISTS (
    SELECT 1 FROM dbo.doc_gia
    WHERE ma_doc_gia = 'DG-DEMO01'
)
BEGIN
    INSERT INTO dbo.doc_gia(
        ma_doc_gia,
        ho_ten,
        ma_dinh_danh,
        don_vi,
        email,
        sdt
    )
    VALUES (
        'DG-DEMO01',
        N'Nguyễn Minh An',
        'SV-DEMO-001',
        N'Công nghệ thông tin',
        'minhan@example.edu.vn',
        '0900000001'
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.doc_gia
    WHERE ma_doc_gia = 'DG-DEMO02'
)
BEGIN
    INSERT INTO dbo.doc_gia(
        ma_doc_gia,
        ho_ten,
        ma_dinh_danh,
        don_vi,
        email,
        sdt
    )
    VALUES (
        'DG-DEMO02',
        N'Trần Thu Hà',
        'SV-DEMO-002',
        N'Ngôn ngữ Anh',
        'thuha@example.edu.vn',
        '0900000002'
    );
END;
GO

/* 19. DU LIEU MAU - THE DOC GIA */

IF NOT EXISTS (
    SELECT 1 FROM dbo.the_doc_gia
    WHERE ma_the = 'THE-DEMO01'
)
BEGIN
    INSERT INTO dbo.the_doc_gia(
        ma_the,
        doc_gia_id,
        ngay_cap,
        ngay_het_han,
        trang_thai
    )
    SELECT
        'THE-DEMO01',
        id,
        CAST(GETDATE() AS DATE),
        DATEADD(MONTH, 12, CAST(GETDATE() AS DATE)),
        'HIEU_LUC'
    FROM dbo.doc_gia
    WHERE ma_doc_gia = 'DG-DEMO01';
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.the_doc_gia
    WHERE ma_the = 'THE-DEMO02'
)
BEGIN
    INSERT INTO dbo.the_doc_gia(
        ma_the,
        doc_gia_id,
        ngay_cap,
        ngay_het_han,
        trang_thai
    )
    SELECT
        'THE-DEMO02',
        id,
        CAST(GETDATE() AS DATE),
        DATEADD(MONTH, 12, CAST(GETDATE() AS DATE)),
        'HIEU_LUC'
    FROM dbo.doc_gia
    WHERE ma_doc_gia = 'DG-DEMO02';
END;
GO

/* 20. DU LIEU MAU - PHIEU MUON*/

IF NOT EXISTS (
    SELECT 1 FROM dbo.phieu_muon
    WHERE ma_phieu = 'PM-DEMO01'
)
BEGIN
    INSERT INTO dbo.phieu_muon(
        ma_phieu,
        doc_gia_id,
        ngay_muon,
        han_tra,
        trang_thai
    )
    SELECT
        'PM-DEMO01',
        id,
        CAST(GETDATE() AS DATE),
        DATEADD(DAY, 14, CAST(GETDATE() AS DATE)),
        'DANG_MUON'
    FROM dbo.doc_gia
    WHERE ma_doc_gia = 'DG-DEMO01';
END;
GO

/* 21. DU LIEU MAU - CHI TIET PHIEU MUON*/

IF NOT EXISTS (
    SELECT 1
    FROM dbo.ct_phieu_muon ct
    JOIN dbo.phieu_muon pm ON ct.phieu_muon_id = pm.id
    JOIN dbo.ban_sao_sach bs ON ct.ban_sao_sach_id = bs.id
    WHERE pm.ma_phieu = 'PM-DEMO01'
      AND bs.ma_vach = 'LIB-001-002'
)
BEGIN
    INSERT INTO dbo.ct_phieu_muon(
        phieu_muon_id,
        ban_sao_sach_id,
        trang_thai
    )
    SELECT
        pm.id,
        bs.id,
        'DANG_MUON'
    FROM dbo.phieu_muon pm
    CROSS JOIN dbo.ban_sao_sach bs
    WHERE pm.ma_phieu = 'PM-DEMO01'
      AND bs.ma_vach = 'LIB-001-002';
END;
GO

/*   21. KIEM TRA DU LIEU SAU KHI TAO */

SELECT * FROM dbo.the_loai;
SELECT * FROM dbo.nha_xuat_ban;
SELECT * FROM dbo.tac_gia;
SELECT * FROM dbo.ke_sach;
SELECT * FROM dbo.sach;
SELECT * FROM dbo.sach_tac_gia;
SELECT * FROM dbo.ban_sao_sach;
SELECT * FROM dbo.doc_gia;
SELECT * FROM dbo.the_doc_gia;
SELECT * FROM dbo.phieu_muon;
SELECT * FROM dbo.ct_phieu_muon;

PRINT N'HOAN THANH TAO DATABASE SGU_LIBRARY!';
GO
