# Đồ án hệ thống quản lý thư viện

Đồ án môn **Phân tích thiết kế hướng đối tượng**: ứng dụng quản lý thư viện bằng Java Swing, JDBC và Microsoft SQL Server.


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
- Microsoft SQL Server 2017 trở lên
- Microsoft JDBC Driver for SQL Server khai báo trong `pom.xml`

## Cài đặt CSDL và chạy chương trình

1. Khởi động SQL Server.
2. Tại thư mục gốc repo, chạy `database/library_catalog.sql` bằng SQL Server Management Studio hoặc `sqlcmd` (dùng Windows Authentication).

```powershell
sqlcmd -S localhost -E -f i:65001 -i database\library_catalog.sql
```

3. Trong SSMS, tạo login riêng cho ứng dụng (không dùng `sa`):
   - Mở **Security → Logins**, nhấp phải **Logins → New Login...**.
   - Chọn **SQL Server authentication**, đặt **Login name** là `library_app`, rồi tự đặt và xác nhận mật khẩu mạnh. Để **Enforce password policy** được chọn.
   - Đặt **Default database** là `sgu_library`.
   - Mở trang **User Mapping**, chọn `sgu_library`; ở **Database role membership**, chỉ chọn `db_datareader` và `db_datawriter` (ngoài `public` mặc định).
   - Nhấn **OK**. SQL Server phải bật **SQL Server and Windows Authentication mode**. Nếu vừa bật chế độ này, khởi động lại dịch vụ SQL Server.

   Các quyền trên cho phép ứng dụng đọc/ghi dữ liệu thư viện; không cấp quyền quản trị server. Chạy script schema ở bước 2 bằng Windows Authentication trước khi chạy ứng dụng.

4. Cấu hình ứng dụng và tiện ích tạo Admin bằng các biến môi trường PowerShell. Dùng mật khẩu SQL vừa đặt cho `library_app`:

```powershell
$env:LIBRARY_DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=sgu_library;encrypt=true;trustServerCertificate=true"
$env:LIBRARY_DB_USER = "library_app"
$env:LIBRARY_DB_PASSWORD = "MAT_KHAU_SQL_CUA_LIBRARY_APP"
```

Thay `localhost:1433` nếu server/port SQL Server của bạn khác; ví dụ phải khớp với instance đã cấu hình trong SSMS.

5. Tạo hoặc khôi phục tài khoản Admin duy nhất bằng tiện ích. Tài khoản mặc định là `Admin`, mật khẩu `admin@12345`; mật khẩu được lưu dưới dạng PBKDF2 hash. Chạy lại tiện ích sẽ đặt lại Admin về thông tin mặc định và mở khóa tài khoản:

```powershell
mvn clean compile exec:java@admin-bootstrap
```

6. Khởi chạy ứng dụng:

```powershell
mvn clean compile exec:java
```

Nếu không kết nối được, kiểm tra dịch vụ SQL Server, TCP/IP và cổng 1433, tên database, chế độ xác thực, user và mật khẩu. `LIBRARY_DB_URL` có thể thay đổi theo instance/cấu hình môi trường.

Sau lần đăng nhập đầu, Admin có thể đổi mật khẩu trong ứng dụng. Tài khoản Admin có tên cố định và không thể tạo/sửa trong màn hình quản lý tài khoản; Admin tạo một tài khoản riêng cho từng Thủ thư hoặc Độc giả.

Tài khoản Admin quản lý tài khoản và phân quyền; Thủ thư quản lý sách, độc giả, mượn/trả và phiếu phạt; Độc giả chỉ tra cứu và xem hồ sơ/lịch sử của mình. Đăng nhập sai liên tiếp 6 lần sẽ khóa tài khoản. Chức năng QR ghi nhận thanh toán do thủ thư xác nhận, không tích hợp cổng thanh toán.

## Các luồng thử thủ công

- **Tra cứu:** thử `LIB-001` hoặc `Nguyễn Nhật Ánh`.
- **Độc giả/thẻ:** mở **Độc giả/thẻ**; dữ liệu mẫu có `DG-DEMO01`, `DG-DEMO02`, thẻ `THE-DEMO01`, `THE-DEMO02`.
- **Mượn:** mở **Mượn/trả**, nhập `DG-DEMO01` và mã vạch sẵn sàng `LIB-001-001`; có thể nhập nhiều mã, phân cách bằng dấu phẩy.
- **Trả:** chọn lượt đang mượn, bấm **Ghi nhận trả cuốn đã chọn**, rồi chọn tốt/hỏng/mất.
- **Bản sao:** mở **Quản lý bản sao** để xem/thêm mã vạch hoặc đánh dấu hỏng/mất.
- **Báo cáo:** mở **Báo cáo** để xem tổng quan, top 10 và xuất CSV.
- **Tài khoản/phân quyền:** đăng nhập `Admin`, tạo tài khoản riêng cho Thủ thư hoặc liên kết tài khoản Độc giả với hồ sơ chưa liên kết; thử khóa/mở khóa và đăng nhập sai 6 lần.
- **Phạt/công nợ:** tạo chi tiết vi phạm, xác nhận thanh toán tiền mặt/QR; thử mượn hoặc gia hạn thẻ khi còn khoản chưa thanh toán.

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




