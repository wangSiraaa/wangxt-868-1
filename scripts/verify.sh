#!/usr/bin/env bash
# =======================================================
# 工业机器人租赁维修结算 - 端到端验收脚本
# 场景：录入维修停机时段并验证租金扣减
# =======================================================
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="$BASE_URL/api"

red()    { echo -e "\033[31m$*\033[0m"; }
green()  { echo -e "\033[32m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
blue()   { echo -e "\033[34m$*\033[0m"; }
bold()   { echo -e "\033[1m$*\033[0m"; }

pass=0
fail=0

assert_eq() {
  local name="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    green "  ✓ PASS: $name  (expected=$expected)"
    pass=$((pass+1))
  else
    red "  ✗ FAIL: $name"
    red "    expected: $expected"
    red "    actual:   $actual"
    fail=$((fail+1))
  fi
}

assert_contains() {
  local name="$1" needle="$2" haystack="$3"
  if echo "$haystack" | grep -q "$needle"; then
    green "  ✓ PASS: $name  (contains '$needle')"
    pass=$((pass+1))
  else
    red "  ✗ FAIL: $name - 未找到 '$needle'"
    red "    response: $haystack"
    fail=$((fail+1))
  fi
}

json_get() {
  python3 -c "import json,sys; d=json.load(sys.stdin); $1" <<<"$2"
}

bold "=========================================================="
bold "工业机器人租赁维修结算 - 端到端验收"
bold "场景：录入维修停机时段并验证租金扣减"
bold "=========================================================="

# ----------------------------- a) 等待服务就绪
echo
blue "[a] 等待后端就绪..."
for i in $(seq 1 60); do
  if curl -s "$API/health" | grep -q "UP"; then
    green "  后端健康检查通过 (${i}s)"
    break
  fi
  echo -n "."
  sleep 2
  [ $i -eq 60 ] && { red "  后端未就绪，超时"; exit 1; }
done

# ----------------------------- b) 创建机器人
echo
blue "[b] 创建机器人 RB001，小时费率 100 元/小时"
resp=$(curl -s -X POST "$API/robots"   -H "Content-Type: application/json"   -d '{
    "robotCode": "RB001",
    "robotName": "焊接机器人-WelderA",
    "model": "Welder-X100",
    "manufacturer": "测试机器人厂商",
    "status": "IDLE",
    "hourlyRate": 100
  }')
assert_contains "创建机器人成功" '"code":0\|"code":200' "$resp" || { red "响应: $resp"; }
ROBOT_ID=$(json_get "print(d.get('data',{}).get('id','') or d.get('data',{}).get('robotId',''))" "$resp")
[ -z "$ROBOT_ID" ] && ROBOT_ID=1
yellow "  机器人 ID=$ROBOT_ID"

# ----------------------------- c) 创建并激活租赁单
echo
blue "[c] 创建并激活租赁订单：SH-AutoParts 工厂"
resp=$(curl -s -X POST "$API/lease-orders"   -H "Content-Type: application/json"   -d '{
    "orderNo": "LO-2025-0001",
    "lesseeFactory": "SH-AutoParts 上海汽配制造有限公司",
    "contactPerson": "张工程师",
    "contactPhone": "13800000001",
    "robotId": '"$ROBOT_ID"',
    "startTime": "2025-01-15T00:00:00",
    "status": "DRAFT"
  }')
assert_contains "创建租赁单成功" '"code":0\|"code":200' "$resp"
ORDER_ID=$(json_get "print(d.get('data',{}).get('id',''))" "$resp")
[ -z "$ORDER_ID" ] && ORDER_ID=1
yellow "  租赁单 ID=$ORDER_ID"

echo "  激活订单..."
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/activate" -H "Content-Type: application/json")
assert_contains "激活成功" '"code":0\|"code":200\|ACTIVE' "$resp"

# ----------------------------- d) 上报 1月15日 8小时
echo
blue "[d] 上报 2025-01-15 运行 8 小时"
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/running-hours"   -H "Content-Type: application/json"   -d '{"reportDate": "2025-01-15", "reportedHours": 8}')
assert_contains "上报成功" '"code":0\|"code":200' "$resp"
# 获取当日记录
RH1_ID=$(json_get "print(d.get('data',{}).get('id',''))" "$resp")

# ----------------------------- e) 上报 1月16日 12小时
echo
blue "[e] 上报 2025-01-16 运行 12 小时"
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/running-hours"   -H "Content-Type: application/json"   -d '{"reportDate": "2025-01-16", "reportedHours": 12}')
assert_contains "上报成功" '"code":0\|"code":200' "$resp"

# ----------------------------- f) 登记维修 1月15日 08:00-12:00 停机4小时
echo
blue "[f] 登记维修：2025-01-15 08:00~12:00，停机 4 小时，费用 500 元"
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/maintenance"   -H "Content-Type: application/json"   -d '{
    "startTime": "2025-01-15T08:00:00",
    "endTime":   "2025-01-15T12:00:00",
    "description": "减速器故障更换，停机4小时",
    "maintenanceCost": 500,
    "status": "COMPLETED"
  }')
assert_contains "维修登记成功" '"code":0\|"code":200' "$resp"
MT1_ID=$(json_get "print(d.get('data',{}).get('id',''))" "$resp")
sleep 1

# ----------------------------- g) 验证 1月15日扣减
echo
blue "[g] ★ 关键验证：查询 2025-01-15 运行小时，应扣减 4h → 计费 4h（8-4）"
resp=$(curl -s "$API/lease-orders/$ORDER_ID/running-hours/all")
# 解析所有记录中日期为 01-15 的
data=$(json_get "
xs=d.get('data',[])
for x in xs:
    rd=str(x.get('reportDate',''))
    if '01-15' in rd:
        rh=float(x.get('reportedHours',0))
        dh=float(x.get('deductionHours',0))
        bh=float(x.get('billableHours',0))
        print(f'DATE={rd} REPORTED={rh} DEDUCT={dh} BILLABLE={bh}')
        break
" "$resp")
yellow "  $data"
ACTUAL_DEDUCT=$(echo "$data" | sed -n 's/.*DEDUCT=\([0-9.]*\).*/\1/p')
ACTUAL_BILLABLE=$(echo "$data" | sed -n 's/.*BILLABLE=\([0-9.]*\).*/\1/p')
assert_eq "1月15日 deductionHours (扣减小时)" "4.0" "${ACTUAL_DEDUCT}"
assert_eq "1月15日 billableHours  (计费小时=8-4)" "4.0" "${ACTUAL_BILLABLE}"

# ----------------------------- h) 验证 1月16日不变
echo
blue "[h] 验证：2025-01-16 计费小时不变（12h）"
data=$(json_get "
xs=d.get('data',[])
for x in xs:
    rd=str(x.get('reportDate',''))
    if '01-16' in rd:
        rh=float(x.get('reportedHours',0))
        dh=float(x.get('deductionHours',0))
        bh=float(x.get('billableHours',0))
        print(f'DATE={rd} REPORTED={rh} DEDUCT={dh} BILLABLE={bh}')
        break
" "$resp")
yellow "  $data"
ACTUAL_BILLABLE_16=$(echo "$data" | sed -n 's/.*BILLABLE=\([0-9.]*\).*/\1/p')
assert_eq "1月16日 billableHours（无维修，应为12）" "12.0" "${ACTUAL_BILLABLE_16}"

# ----------------------------- i) 计算结算单
echo
blue "[i] 计算结算单：总计费小时 16h，租金 1600 元，维修费 500 元，总计 2100 元"
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/settlement/calculate"   -H "Content-Type: application/json")
assert_contains "结算计算成功" '"code":0\|"code":200' "$resp"
echo "  $resp" | head -c 500; echo
TBH=$(json_get "print(d.get('data',{}).get('totalBillableHours',0))" "$resp")
BR=$(json_get  "print(d.get('data',{}).get('baseRent',0))"          "$resp")
MT=$(json_get  "print(d.get('data',{}).get('maintenanceTotal',0))"  "$resp")
TA=$(json_get  "print(d.get('data',{}).get('totalAmount',0))"       "$resp")
ST=$(json_get  "print(d.get('data',{}).get('status',''))"           "$resp")
yellow "  totalBillableHours=$TBH  baseRent=$BR  maintenanceTotal=$MT  totalAmount=$TA  status=$ST"
assert_eq "总计费小时（4+12）"  "16.0" "${TBH}"
assert_eq "基础租金（16h×100）" "1600.0" "${BR}"
assert_eq "维修费用合计"         "500.0"  "${MT}"
assert_eq "结算总金额（1600+500）" "2100.0" "${TA}"
assert_eq "结算状态" "DRAFT" "${ST}"

# ----------------------------- j) 复核
echo
blue "[j] 复核结算单 → REVIEWED"
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/settlement/review"   -H "Content-Type: application/json")
assert_contains "复核成功" '"code":0\|"code":200' "$resp"
ST2=$(json_get "print(d.get('data',{}).get('status',''))" "$resp")
assert_eq "结算状态变为 REVIEWED" "REVIEWED" "${ST2}"

# ----------------------------- k) 确认结算
echo
blue "[k] 确认结算 → CONFIRMED（锁定）"
resp=$(curl -s -X POST "$API/lease-orders/$ORDER_ID/settlement/confirm"   -H "Content-Type: application/json")
assert_contains "确认成功" '"code":0\|"code":200' "$resp"
ST3=$(json_get "print(d.get('data',{}).get('status',''))" "$resp")
assert_eq "结算状态变为 CONFIRMED（锁定）" "CONFIRMED" "${ST3}"

# ----------------------------- l) 锁定验证：修改维修记录应失败
echo
blue "[l] ★ 锁定验证：结算确认后，修改维修记录应被拒绝（403/400）"
resp=$(curl -s -w "\nHTTP_CODE:%{http_code}\n" -X PUT   "$API/lease-orders/$ORDER_ID/maintenance/$MT1_ID"   -H "Content-Type: application/json"   -d '{
    "startTime": "2025-01-15T09:00:00",
    "endTime":   "2025-01-15T11:00:00",
    "description": "尝试修改（应被拒绝）",
    "maintenanceCost": 0,
    "status": "COMPLETED"
  }')
HTTP_CODE=$(echo "$resp" | tail -1 | sed -n 's/.*HTTP_CODE:\([0-9]*\).*/\1/p')
yellow "  HTTP=$HTTP_CODE"
if [ "$HTTP_CODE" = "200" ] && echo "$resp" | grep -q '"code":0\|"code":200'; then
  red "  ✗ FAIL: 锁定后修改维修记录居然成功了！（预期应被拒绝）"
  fail=$((fail+1))
else
  green "  ✓ PASS: 锁定后修改维修记录被正确拒绝 (HTTP=${HTTP_CODE:-非200})"
  pass=$((pass+1))
fi

# ----------------------------- m) 锁定验证：删除维修记录应失败
echo
blue "[m] ★ 锁定验证：结算确认后，删除维修记录应被拒绝"
resp=$(curl -s -w "\nHTTP_CODE:%{http_code}\n" -X DELETE   "$API/lease-orders/$ORDER_ID/maintenance/$MT1_ID")
HTTP_CODE=$(echo "$resp" | tail -1 | sed -n 's/.*HTTP_CODE:\([0-9]*\).*/\1/p')
yellow "  HTTP=$HTTP_CODE"
if [ "$HTTP_CODE" = "200" ] && echo "$resp" | grep -q '"code":0\|"code":200'; then
  red "  ✗ FAIL: 锁定后删除维修记录居然成功了！（预期应被拒绝）"
  fail=$((fail+1))
else
  green "  ✓ PASS: 锁定后删除维修记录被正确拒绝 (HTTP=${HTTP_CODE:-非200})"
  pass=$((pass+1))
fi

# ----------------------------- 汇总
echo
bold "=========================================================="
bold "验收汇总"
bold "=========================================================="
green "通过: $pass"
if [ "$fail" -gt 0 ]; then
  red "失败: $fail"
  exit 1
else
  green "失败: 0"
  green "🎉 所有验收项均通过！"
  exit 0
fi
