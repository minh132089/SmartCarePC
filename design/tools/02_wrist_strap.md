# 02 — Dây đeo cổ tay chống tĩnh điện

Ảnh: `02_wrist_strap.png`. Dùng ở bước 0, trước khi chạm CPU, RAM, bo mạch hoặc card. Các số dưới đây là kích thước chọn cho mô hình PC SmartCare, không phải bản vẽ chế tạo hay số đo sản phẩm thương mại. **Dựng theo đặc tả này, không đo trên ảnh.**

## Kích thước và tọa độ

Đơn vị mm. Gốc O ở tâm vòng đeo khi đặt nghỉ; +X chạy từ vòng đeo về đầu kẹp, +Y hướng lên, +Z hướng ra người xem FRONT. Vòng nằm trong mặt phẳng XZ, bề rộng theo Y. Tọa độ là tâm bộ phận, kích thước theo X × Y × Z nếu không ghi khác.

- Bao bộ ở tư thế nghỉ: **470 × 24 × 64**, X = −38…432. Chiều dài này không phải chiều dài dây khi kéo giãn.
- Vòng mẫu: đường kính ngoài 64, trong 60; bản rộng 20, dày 2. Chu vi ôm tay điều chỉnh mục tiêu 140–220; tư thế mẫu có chu vi trong khoảng 188,5.
- Dây xoắn: chiều dài sử dụng khi kéo giãn khoảng **1.800**; đường kính vỏ dây 2,2. Cuộn nghỉ dài 160, đường kính ngoài 15, khoảng 40 vòng, bước xoắn 4. Dây mềm đổi hình khi đeo; không kéo thẳng cứng trong cảnh thao tác.

| Số | Bộ phận / số lượng | Vị trí và hình học |
|---|---|---|
| 1 | Đai đàn hồi dẫn điện ×1 | Tâm O; vành tròn ngoài R32, trong R30; Y = −10…10. Có đầu đai luồn khóa, cho phép chỉnh chu vi. |
| 2 | Bản tiếp xúc da ×1 | Tâm (29,5; 0; 0), bao 1 × 18 × 22; uốn theo lòng vòng R30, bo góc R2. Nằm ngay phía trong đế snap, nối điện qua đinh tán xuyên đai. |
| 3 | Khóa chỉnh đai ×1 | Tâm (−33; 0; 0), bao 10 × 24 × 24; hai khe luồn đai rộng 21, khe hở 2,5. Khóa ở phía đối diện cụm tiếp xúc. |
| 4 | Cụm snap tháo được ×1 | Đế nhựa tâm (35,5; 0; 0), bao 11 × 24 × 24. Trục snap dọc X; chốt ghép Ø4, nắp ngoài Ø10; phần đầu nối X = 41…45. Một cặp đực/cái, một đinh tán nối bản số 2. |
| 5 | Điện trở nối tiếp ×1, vỏ kín ×1 | Vỏ tâm (59; 0; 0), bao 28 × 9 × 9, X = 45…73. Điện trở **1 MΩ**, thân minh họa dài 6 × Ø2,5, đặt dọc X ở giữa vỏ; hai đầu nối kín, có chống gập. |
| 6 | Dây dẫn xoắn ×1 | Đuôi đầu X = 73…173; cuộn xoắn X = 173…333, trục (X; 0; 0); đuôi cuối X = 333…382. Hai đoạn chuyển tiếp uốn tiếp tuyến với cuộn, không gãy góc. |
| 7 | Kẹp cá sấu ×1 | Tâm (407; 0; 0), bao 50 × 16 × 12; mũi hướng +X. Hai má thép có răng dài 18, một chốt bản lề tâm (407; 0; 0), trục Z, một lò xo hồi; miệng mở tối đa 10 theo Y. Hai tay bóp có áo nhựa, răng kẹp để trần. |

Đường điện liên tục: da → bản số 2 → snap → điện trở 1 MΩ → dây xoắn → hai má kẹp → khung case. Điện trở nằm trong vỏ kín gần cổ tay; chỉ ô giải thích mới cắt vỏ để nhìn vào.

## Vật liệu và màu

Đai dệt đàn hồi có sợi dẫn điện: xanh `#287AB0`, mặt trong xanh đậm `#215C7D`. Khóa và đế snap: nhựa nhám `#202329`. Vỏ dây và chống gập: PU mềm `#17191D`. Bản da, snap, chốt và má kẹp: thép không gỉ/thép mạ sáng `#B9C0C6`. Thân điện trở minh họa: be `#C5AA78`. Không logo, không đèn.

Ô sử dụng có khung thép case `#24262A`, mép tiếp xúc kim loại trần `#AEB4BC`, dày 1,2 theo các họ vỏ; Gaming dùng mép dày 1. Răng kẹp ôm hai mặt mép, không xuyên tấm. Tay và case chỉ là ngữ cảnh, không tính vào số món của bộ dây.

## Người mới cần biết

1. Đeo trước khi lấy linh kiện khỏi túi chống tĩnh điện. Đai giúp giảm chênh lệch điện tích giữa người và khung máy.
2. Chỉnh vừa cổ tay; bản kim loại phải chạm trực tiếp da, không nằm trên tay áo.
3. Kẹp vào mép kim loại trần của khung case. Lớp sơn, kính và nhựa không phải điểm tiếp xúc phù hợp; không kẹp lên bo mạch.
4. Tắt máy và rút dây điện ngoài trước khi lắp. Kẹp vào case đã rút điện không đồng nghĩa nối đất bảo vệ; không cắm kẹp vào ổ điện hoặc chân C14.
5. Dùng bộ dây ESD có điện trở 1 MΩ nguyên vẹn; không nối tắt điện trở. Dây đeo không bảo vệ khỏi điện giật. Tháo dây đeo trước khi cấp điện thử máy, không mở PSU.

## Sai khác của ảnh

Ảnh có đủ FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM, góc 3/4 và ô sử dụng, nhưng các hình chiếu chưa đồng nhất tỷ lệ. LEFT/RIGHT tách mặt đầu cuộn sang cạnh vòng, không phải phép chiếu lắp đúng; TOP/BOTTOM chưa đảo hướng nhất quán. Vị trí khóa và snap giữa các hình cũng chưa khớp tọa độ trên. Ở góc 3/4, bản tiếp xúc bị vẽ sang thành đối diện: khi dựng phải đặt ngay sau snap theo mục 2. Các đường kích thước, số vòng xoắn và vạch màu điện trở chỉ minh họa; giá trị đúng là bảng trên và **1 MΩ**. Ảnh đã sửa một lần theo brief.

Ảnh được tạo bằng công cụ imagegen tích hợp. Nội dung lệnh: tờ tham chiếu nền trắng, sáu hình chiếu cùng tỷ lệ, một góc 3/4, đai xanh có bản da và khóa chỉnh, dây xoắn khoảng 1.800 mm với điện trở kín 1 MΩ, kẹp cá sấu trên mép khung trần; đường kích thước xanh, nhãn tiếng Anh ngắn, không thương hiệu. Lần sửa yêu cầu chỉnh hình chiếu, vị trí bản da và nhãn vỏ điện trở 28 × 9 × 9.
