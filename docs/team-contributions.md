# Báo Cáo Phân Công Công Việc Trong Nhóm (Team Contributions)

Dự án **SmartExpApp** được phát triển và vận hành dựa trên sự phân chia vai trò rõ ràng và làm việc nhóm có hệ thống. Các thành viên phụ trách từng phân hệ chuyên môn hóa nhằm tối ưu hiệu năng phát triển và đảm bảo chất lượng phần mềm.

## 1. Vai trò và Phân công Tổng quan

| Thành viên (Handle) | Vai trò chính | Nhiệm vụ trọng tâm |
| :--- | :--- | :--- |
| **@thqhuong** | Nhóm trưởng / Lead Developer <br> & Main Merge Reviewer | - Quản lý nhánh, kiểm duyệt chính các Pull Requests.<br>- Thiết kế cấu trúc hệ thống, đồng bộ Firestore Cloud.<br>- Tích hợp Xác thực (Authentication) & AI Worker.<br>- Đóng gói bản Release APK. |
| **@huyqt3062006** | Core Feature Developer | - Phát triển các tính năng quản lý sản phẩm cơ bản (Product Actions).<br>- Xây dựng màn hình Nhật ký & Lịch sử sản phẩm.<br>- Thiết kế bộ lọc Dropdown & Tính năng chọn nhiều sản phẩm.<br>- Xử lý cơ chế Hoàn tác (Undo). |
| **@duongnguyenphuquy17052000** | Senior Developer / QA Engineer | - Phát triển màn hình Dashboard, phân tích & hiển thị biểu đồ Room-backed.<br>- Bản địa hóa ngôn ngữ (i18n), định dạng ngày tháng.<br>- Tối ưu hóa Khả năng tiếp cận (Accessibility - a11y) & Fix UI bugs.<br>- Viết kịch bản kiểm thử (Test Cases & Smoke Checklist). |

---

## 2. Chi tiết Đóng góp của Từng Thành viên

### 👤 @thqhuong
Phụ trách mảng **Kiến trúc hệ thống, Bảo mật, Đồng bộ Đám mây & Quản lý Git**:
*   **Quản lý quy trình Git & Merge:** Đóng vai trò là người kiểm duyệt chính (Main Merge Reviewer), thực hiện đánh giá code (code review), xử lý xung đột khi trộn nhánh (merge conflicts) và phê duyệt các Pull Requests lên nhánh chính.
*   **Đồng bộ Firestore Cloud Storage:** Thiết kế và xây dựng nền tảng đồng bộ dữ liệu hai chiều giữa SQLite cục bộ (Room DB) và Firebase Cloud Firestore giúp đồng bộ dữ liệu người dùng trực tuyến.
*   **Xác thực tài khoản (Auth):** Tích hợp Firebase Auth và Android Credential Manager cho phép đăng nhập One-Tap và quản lý tài khoản cục bộ.
*   **Tích hợp AI & Thông báo:** Thiết kế hệ thống thông báo trong ứng dụng, phát triển hộp thoại chọn nhiều sản phẩm (Smart Add) và kết nối Cloudflare Workers AI.
*   **DevOps & Đóng gói:** Bảo trì cấu hình dự án, dọn dẹp các tệp dư thừa, cấu hình Gradle và biên dịch bản Release APK hoàn chỉnh.

### 👤 @huyqt3062006
Phụ trách mảng **Tương tác & Quản lý Sản phẩm (Product Operations)**:
*   **Bộ điều hướng hành động sản phẩm (Product Actions):** Xây dựng giao diện Bottom Sheet, hộp thoại xác nhận xóa và thay đổi trạng thái sử dụng của thực phẩm.
*   **Nhật ký & Lịch sử:** Triển khai màn hình Lịch sử sản phẩm giúp lưu trữ và hiển thị quá trình sử dụng thực phẩm của gia đình.
*   **UI Controls nâng cao:** Xây dựng bộ lọc dropdown phân loại thực phẩm, bộ lọc mở rộng và tính năng đa nhiệm (Multi-select) cho phép người dùng thao tác nhanh nhiều thực phẩm cùng lúc.
*   **Tối ưu UX:** Tích hợp tính năng Hoàn tác nhanh (Undo) khi xóa hoặc đổi trạng thái sản phẩm để nâng cao trải nghiệm sử dụng thực tế.

### 👤 @duongnguyenphuquy17052000
Phụ trách mảng **Phân tích số liệu, Bản địa hóa, Tối ưu giao diện & Đảm bảo chất lượng (QA)**:
*   **Dashboard & Thống kê dữ liệu:** Xây dựng màn hình Dashboard hiển thị biểu đồ tròn và thống kê tiêu thụ từ cơ sở dữ liệu Room cục bộ.
*   **Bản địa hóa (Localization - i18n):** Chuẩn hóa định dạng ngày hiển thị (`dd/MM/yyyy`) cho toàn bộ ứng dụng và dịch thuật ngôn ngữ các khu vực lưu trữ (Storage Preferences).
*   **Khả năng tiếp cận (Accessibility - a11y):** Tối ưu vùng chạm (Touch targets), bổ sung mô tả nội dung (`content descriptions`) cho người khiếm thị và dọn dẹp các cảnh báo Lint.
*   **Kiểm thử & Đảm bảo chất lượng:** Xây dựng các test cases tự động cho Reminder Scheduler, kiểm tra cấu trúc Storage Contract và chuẩn bị danh sách kịch bản kiểm thử thủ công (manual smoke checklist).
*   **Pháp lý & Tài liệu:** Viết chính sách bảo mật thông tin (Privacy Policy) và hướng dẫn bảo mật khi sử dụng AI.

---

## 3. Quy trình Phối hợp (Workflow)
1.  **Phát triển nhánh tính năng:** Các thành viên phát triển tính năng độc lập trên các nhánh riêng (`feature/`, `fix/`, `docs/`).
2.  **Đánh giá mã nguồn (Code Review):** Các thay đổi phải thông qua Pull Request, được kiểm duyệt bởi **@thqhuong** trước khi trộn vào nhánh phát triển chung (`dev`) và nhánh chính (`master`).
3.  **Sửa lỗi UI & Merge regression:** Khắc phục kịp thời các xung đột giao diện phát sinh sau khi gộp nhánh để đảm bảo luồng hoạt động chuẩn xác của ứng dụng.
