export type PartId = 'case' | 'motherboard' | 'cpu' | 'cooler' | 'ram' | 'gpu' | 'ssd' | 'psu' | 'fans' | 'cables' | 'tools'

// Mô tả viết cho người không rành công nghệ.
export const PARTS: Record<PartId, { name: string; about: string }> = {
  case: { name: "PC case", about: "Holds and protects the components, with fan mounts for cooling." },
  motherboard: {
    name: "Motherboard",
    about: "The main circuit board connects all the components. Each type has its own socket or slot.",
  },
  cpu: { name: "CPU (processor)", about: "Processes instructions and sits beneath the CPU cooler." },
  cooler: { name: "CPU cooler", about: "Removes heat from the CPU to prevent overheating." },
  ram: { name: 'RAM', about: "Temporary working memory. More RAM lets you run more programs at once." },
  gpu: { name: "Graphics card (GPU)", about: "Handles graphics for gaming, video editing and 3D work." },
  ssd: { name: "SSD", about: "Stores your operating system, programs and files." },
  psu: { name: "Power supply (PSU)", about: "Converts mains electricity into power for the components." },
  fans: { name: "Case fans", about: "Bring cool air into the case and push warm air out." },
  tools: {
    name: "Assembly tools",
    about: "Tools for assembly: a screwdriver, anti-static strap, thermal paste, screw organiser, cable ties, cutters and power cable. Select Explode view to see them beside the PC.",
  },
  cables: {
    name: "Cables",
    about:
      "Thick power cables feed the motherboard (24-pin), CPU (8-pin) and GPU (PCIe). Fan cables use FAN headers. Case button leads connect to F_PANEL; the blue cable connects front USB.",
  },
}
