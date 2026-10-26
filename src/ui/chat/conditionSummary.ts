/** Short actions for known checks; the original message remains available in the details. */
export function conditionSummary(message: string, rule?: string): string {
  const vietnamese = /[ĂăÂâĐđÊêÔôƠơƯư\u1ea0-\u1ef9]/u.test(message)
  switch (rule) {
    case 'CPU_SUPPORT_LIST':
      return vietnamese ? 'Xác nhận bo mạch hỗ trợ CPU và phiên bản BIOS.' : 'Confirm CPU support and the required BIOS version.'
    case 'CPU_BIOS_UPDATE':
      return vietnamese ? 'Nhờ cửa hàng kiểm tra, cập nhật BIOS cho CPU.' : 'Ask the store to check and update the BIOS for this CPU.'
    case 'COOLER_MOUNT': {
      const socket = message.match(/\b(?:LGA\s?\d{3,4}|AM[345]|FM[12]\+?|sTRX4|sTR5|TR4)\b/i)?.[0]
      return vietnamese
        ? `Kiểm tra tản có gá cho ${socket ? `socket ${socket}` : 'socket CPU'}.`
        : `Confirm the cooler includes a mounting kit for ${socket ?? 'the CPU socket'}.`
    }
    case 'GPU_CLEARANCE':
      return vietnamese ? 'Kiểm tra card và đầu cáp nguồn có vừa vỏ.' : 'Check that the GPU and its power connector fit the case.'
    case 'FAN_MOUNTS':
      return vietnamese ? 'Kiểm tra số vị trí gắn quạt còn trống.' : 'Check the remaining case fan mounts.'
    default:
      return message.replace(/^(?:Hỏi cửa hàng xác nhận trước khi mua|Ask the store to confirm before buying):\s*/i, '')
  }
}
