# Đồ án hệ thống quản lý thư viện

Đồ án môn **Phân tích thiết kế hướng đối tượng**: ứng dụng quản lý thư viện bằng Java Swing, JDBC và MySQL/MariaDB.


## Phân công công việc

| MSSV | Thành viên | Email | Công việc | Tỷ trọng |
|---|---|---|---|---:|
| 3124411253 | Nguyễn Hoàng Quy | Bổ sung | Đã làm: quản lý sách (thêm/sửa/ẩn), tác giả, thể loại, nhà xuất bản, kệ, bản sao/mã vạch và tra cứu; dựng nền CSDL/kết nối JDBC; xây dựng chức năng nền độc giả/thẻ, mượn/trả và báo cáo tổng quan/top 10/xuất CSV. | 53% |
| 3124411102 | Trần Thiên Hùng | Bổ sung | Làm đăng nhập/đăng xuất/đổi mật khẩu, tài khoản và phân quyền; hoàn thiện điều kiện độc giả/thẻ và kết nối với công nợ. | 15% |
| 3124411287 | Võ Thuận Thiên | Bổ sung | Hoàn thiện nghiệp vụ mượn/trả, xử lý quá hạn/hỏng/mất; làm gia hạn lượt mượn tối đa một lần, thêm 7 ngày; đồng bộ trạng thái bản sao. | 15% |
| 3123411149 | Phan Vinh Khánh | Bổ sung | Làm xử lý vi phạm, tiền phạt, thanh toán/công nợ; hoàn thiện báo cáo theo kỳ và báo cáo phạt, xuất/in; ghép CSDL, dữ liệu mẫu, hướng dẫn chạy và đóng gói. | 17% |
|  | **Tổng** |  |  | **100%** |

## Công nghệ

- JDK 17 trở lên
- Apache Maven 3.9 trở lên
- MySQL 8 hoặc MariaDB tương thích
- MySQL Connector/J khai báo trong `pom.xml`

## Cài đặt CSDL và chạy chương trình

1. Khởi động MySQL/MariaDB.
2. Tại thư mục gốc repo, chạy `database/library_catalog.sql` bằng MySQL Workbench hoặc:

```powershell
mysql -u root -p < database/library_catalog.sql
```

3. Mặc định ứng dụng dùng `jdbc:mysql://localhost:3306/sgu_library`, user `root`, mật khẩu rỗng. Chạy:

```powershell
mvn clean compile exec:java
```

Nếu cần cấu hình khác, đặt biến môi trường trong PowerShell trước khi chạy:

```powershell
$env:LIBRARY_DB_URL = "jdbc:mysql://localhost:3306/sgu_library?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
$env:LIBRARY_DB_USER = "root"
$env:LIBRARY_DB_PASSWORD = "mat-khau-mysql"
mvn clean compile exec:java
```

Nếu không kết nối được, kiểm tra dịch vụ MySQL/MariaDB, cổng, tên database, user và mật khẩu.

## Các luồng thử thủ công

- **Tra cứu:** thử `LIB-001` hoặc `Nguyễn Nhật Ánh`.
- **Độc giả/thẻ:** mở **Độc giả/thẻ**; dữ liệu mẫu có `DG-DEMO01`, `DG-DEMO02`, thẻ `THE-DEMO01`, `THE-DEMO02`.
- **Mượn:** mở **Mượn/trả**, nhập `DG-DEMO01` và mã vạch sẵn sàng `LIB-001-001`; có thể nhập nhiều mã, phân cách bằng dấu phẩy.
- **Trả:** chọn lượt đang mượn, bấm **Ghi nhận trả cuốn đã chọn**, rồi chọn tốt/hỏng/mất.
- **Bản sao:** mở **Quản lý bản sao** để xem/thêm mã vạch hoặc đánh dấu hỏng/mất.
- **Báo cáo:** mở **Báo cáo** để xem tổng quan, top 10 và xuất CSV.

Đây là bước kiểm tra thủ công ban đầu, chưa thay thế kiểm thử đầy đủ. Dữ liệu mẫu có thể đổi sau khi thao tác.

## Cấu trúc chính

```text
database/library_catalog.sql                  Schema và dữ liệu mẫu
src/main/java/vn/sgu/library/                 Cửa sổ chính, JDBC, quản lý sách
src/main/java/vn/sgu/library/controller/      Điều phối nghiệp vụ
src/main/java/vn/sgu/library/dao/             Truy cập dữ liệu
src/main/java/vn/sgu/library/model/           Mô hình dữ liệu
src/main/java/vn/sgu/library/view/            Màn hình độc giả, mượn/trả, báo cáo
pom.xml                                      Cấu hình Maven/Connector-J
```

## Quy ước tích hợp

- `sach` lưu đầu sách; `ban_sao_sach` lưu từng cuốn vật lý có mã vạch riêng.
- Trạng thái bản sao: `SAN_SANG`, `DANG_MUON`, `HONG`, `MAT`; chỉ `SAN_SANG` được mượn.
- Cập nhật phiếu và tình trạng bản sao trong cùng giao dịch; không tạo bảng bản sao trùng.
- Dùng `sach.trang_thai='AN'` để ẩn đầu sách có lịch sử; không ẩn sách còn bản sao đang mượn.
- Quy tắc theo báo cáo: tối đa 5 cuốn đang mượn, hạn 14 ngày, gia hạn lượt mượn tối đa một lần và thêm 7 ngày. Hoàn thiện và kiểm tra nhất quán khi tích hợp.
- Bảng tài khoản/gia hạn/phạt mới phải ghép vào script CSDL chung và giữ khóa ngoại phù hợp.

## Cấu trúc thư mục nộp

Tạo thư mục theo tên nhóm, gồm README/readme.txt có tên đề tài, thành viên, MSSV, email, tỷ lệ công việc; `BaoCao` chứa Word/PDF; `ChuongTrinh` chứa `Source code`, `Database`, `Tham Khao` theo hướng dẫn giảng viên.




