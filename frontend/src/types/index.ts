/** 前后端共享的业务数据类型定义 */

/** 后端 CropBatch 实体 */
export interface CropBatch {
  id: number
  batchNo: string
  farmerId?: number | null
  cropName: string
  variety?: string | null
  landLocation?: string | null
  plantDate?: string | null
  expectHarvestDate?: string | null
  actualHarvestDate?: string | null
  harvestWeight?: number | null
  status: string
  traceCode?: string | null
  qrCodePath?: string | null
  createTime?: string | null
  farmerName?: string | null
  farmerPhone?: string | null
  farmerAddress?: string | null
}

/** 后端 FarmingLog 实体 */
export interface FarmingLog {
  id: number
  batchId?: number | null
  logType: string
  logDate?: string | null
  material?: string | null
  dosage?: string | null
  content?: string | null
  photoPath?: string | null
  createTime?: string | null
  logTypeName?: string | null
  batchNo?: string | null
  cropName?: string | null
  farmerName?: string | null
}

/** 后端 DetectionReport 实体 */
export interface DetectionReport {
  id: number
  batchId?: number | null
  reportNo?: string | null
  institution?: string | null
  reportUrl?: string | null
  reportDate?: string | null
  result?: string | null
  createTime?: string | null
  batchNo?: string | null
  cropName?: string | null
  farmerName?: string | null
}

/** 后端 TraceRecord 实体 */
export interface TraceRecord {
  id: number
  batchId?: number | null
  fromUser?: number | null
  toUser?: number | null
  type: string
  quantity?: number | null
  location?: string | null
  operateTime?: string | null
  remark?: string | null
  fromUserName?: string | null
  toUserName?: string | null
  typeName?: string | null
  batchNo?: string | null
  cropName?: string | null
}

/** 后端 Complaint 实体 */
export interface Complaint {
  id: number
  batchId?: number | null
  complainant?: string | null
  phone?: string | null
  queryCode?: string | null
  type?: string | null
  content?: string | null
  images?: string | null
  status: string
  reply?: string | null
  consumerReply?: string | null
  consumerReplyTime?: string | null
  createTime?: string | null
  replyTime?: string | null
  cropName?: string | null
  batchNo?: string | null
}

/** 后端 SysUser 实体（管理员用户列表） */
export interface SysUser {
  id: number
  username: string
  realName: string
  role: string
  phone?: string | null
  address?: string | null
  avatar?: string | null
  status?: number | null
  createTime?: string | null
}

/** 后端 ScanLog 实体 */
export interface ScanLog {
  id: number
  batchId?: number | null
  traceCode?: string | null
  ip?: string | null
  userAgent?: string | null
  scanTime?: string | null
  cropName?: string | null
  batchNo?: string | null
}

/** 通用分页结构（后端分页接口预留） */
export interface PageResult<T> {
  list: T[]
  total: number
}

/** 分组统计行：Map<String, Object> 查询结果（保留数据库列名） */
export interface StatRow {
  status?: string
  crop_name?: string
  total?: number | string
  cnt?: number | string
}

/** 按天统计行 */
export interface CountRow {
  day?: string
  cnt?: number | string
}

/** 热门批次统计行 */
export interface HotBatchRow {
  crop_name?: string
  batch_no?: string
  cnt?: number | string
}

/** 名称/数值对（农户工作台作物产量） */
export interface NameValue {
  name: string
  value: number | string
}

/** 农户工作台 */
export interface FarmerDashboard {
  totalBatches: number
  plantingCount: number
  harvestedCount: number
  reportCount: number
  cropStats: NameValue[]
  recentBatches: CropBatch[]
}

/** 管理员数据大屏 */
export interface AdminDashboard {
  batchTotal: number
  farmerCount: number
  dealerCount: number
  pendingCount: number
  complaintTotal: number
  scanToday: number
  cropStats: StatRow[]
  complaintStatus: StatRow[]
  trace7days: CountRow[]
  scan7days: CountRow[]
}

/** 可视化大屏 */
export interface BigScreenData {
  batchTotal: number
  farmerCount: number
  dealerCount: number
  scanToday: number
  scanTotal: number
  pendingCount: number
  traceTotal: number
  reportTotal: number
  qualifiedRate: number
  batchStatus: Record<string, number>
  scan7days: CountRow[]
  trace7days: CountRow[]
  cropStats: StatRow[]
  complaintStatus: StatRow[]
  hotBatches: HotBatchRow[]
  recentScans: ScanLog[]
}

/** 经销商工作台 */
export interface DealerDashboard {
  realName?: string
  availableCount: number
  inboundCount: number
  outboundCount: number
  involvedCount: number
  available: CropBatch[]
  recentRecords?: TraceRecord[]
}

/** 管理员用户管理 */
export interface AdminUsersData {
  list: SysUser[]
  allCount: number
  farmerCount: number
  dealerCount: number
}

/** 管理员检测报告 */
export interface AdminReportsData {
  list: DetectionReport[]
  qualifiedCount: number
  unqualifiedCount: number
}

/** 扫码统计 */
export interface ScanStatsData {
  todayCount: number
  scan7days: CountRow[]
  hotBatches: HotBatchRow[]
  scanLogs: ScanLog[]
}

/** 消费者溯源详情 */
export interface TraceDetailData {
  batch: CropBatch
  logs: FarmingLog[]
  records: TraceRecord[]
  reports: DetectionReport[]
  nodeCount: number
}

/** 经销商溯源查询 */
export interface DealerTraceData {
  batch?: CropBatch
  logs?: FarmingLog[]
  records?: TraceRecord[]
  reports?: DetectionReport[]
}

/** 管理员溯源查询 */
export interface AdminTraceData {
  batch?: CropBatch | null
}

/** 投诉提交结果 */
export interface ComplaintSubmitResult {
  id: number
  queryCode?: string
  traceCode?: string
}

/** 农户溯源码管理 */
export interface TraceCodesData {
  batches: CropBatch[]
  baseUrl: string
}

/** 农户检测报告页 */
export interface FarmerReportsData {
  reports: DetectionReport[]
  batches: CropBatch[]
}

/** 农户农事记录页 */
export interface FarmerLogsData {
  logs: FarmingLog[]
  batches: CropBatch[]
}

/** 批次农事记录页 */
export interface BatchLogsData {
  batch: CropBatch
  logs: FarmingLog[]
}

/** 经销商出库页 */
export interface OutboundData {
  list: CropBatch[]
  inboundIds: number[]
}
