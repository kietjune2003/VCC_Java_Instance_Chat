# 💬 ChatApp - Spring Boot Messaging System

Một ứng dụng chat thời gian thực đơn giản sử dụng Spring Boot, JWT Authentication, BCrypt, MySQL và Long Polling. Phù hợp cho các dự án học thuật, demo bảo vệ, hoặc làm nền tảng mở rộng các ứng dụng trò chuyện.

---

## 🚀 Tính năng chính

- 🔐 Đăng ký / Đăng nhập bảo mật bằng **JWT + BCrypt**
- 💬 Gửi tin nhắn văn bản & tệp đính kèm giữa người dùng
- 🟢 Hiển thị trạng thái online của bạn bè
- 📡 **Long Polling API** cho giao tiếp gần thời gian thực
- 🧾 Lưu lịch sử chat trong MySQL
- 🔁 Tối đa 2 phiên đăng nhập / người dùng
- 📂 Quản lý tệp đính kèm an toàn

---

## 🏗️ Kiến trúc hệ thống

```
Client (React hoặc Postman) <--> Spring Boot API <--> MySQL
                                 |
                              Local Storage (file upload)
```

---

## 🔐 Bảo mật

- Sử dụng **JWT Token + User-Agent Binding** để xác minh thiết bị
- Mật khẩu người dùng được mã hoá bằng **BCrypt**
- Kiểm soát tối đa **2 phiên đăng nhập** cùng lúc
- Tự động xoá token cũ nếu vượt quá số lượng cho phép

---

## 🧪 Hướng dẫn chạy & test

```bash
# Bước 1: Clone dự án
git clone https://github.com/kietjune2003/VCC_Java_Instance_Chat.git
cd chatapp

# Bước 2: Cấu hình database trong application.properties
# Ví dụ:
# spring.datasource.username=root
# spring.datasource.password=123456
# spring.datasource.url=jdbc:mysql://localhost:3306/chatapp

# Bước 3: Build & chạy ứng dụng
./mvnw clean package
java -jar target/chatapp-1.0-SNAPSHOT.jar
```

> ✅ Sử dụng Postman hoặc giao diện React để kiểm thử API

---

## 🛠️ Công nghệ sử dụng

| Thành phần       | Công nghệ        |
|------------------|------------------|
| Backend          | Spring Boot 3    |
| Auth             | JWT, BCrypt      |
| Database         | MySQL 8          |
| Build tool       | Maven            |
| Logging          | SLF4J + Logback  |
| Testing          | JUnit + Mockito  |

---

## 📁 Cấu trúc thư mục

```
📦com.example.chat
 ┣ 📂controller         # REST APIs
 ┣ 📂entity             # JPA Entities
 ┣ 📂repository         # Repositories (JPA)
 ┣ 📂service            # Business Logic
 ┣ 📂util               # JWT Utils, Helpers
 ┣ 📜application.properties
 ┗ 📜ChatAppApplication.java
```

---

## 💡 Mở rộng tiềm năng

- ✅ Nâng cấp thành WebSocket thay vì Long Polling
- 📲 Tích hợp giao diện React, Android hoặc Flutter
- 🌍 Hỗ trợ chat nhóm, emoji, ảnh đại diện, trạng thái hoạt động

---

## 👨‍💻 Tác giả

- **Tên**: kietjune2003
- **Email**: k.code.2003@gmail.com
- **Github**: [https://github.com/kietjune2003](https://github.com/kietjune2003)

> Nếu bạn thấy dự án hữu ích, hãy ⭐️ ủng hộ nhé!