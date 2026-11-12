import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Boxes, ShoppingBag, Truck, Warehouse } from 'lucide-react'
import {
  useCheckoutStoreOrderMutation,
  useCreateInventoryItemMutation,
  useCreatePurchaseOrderMutation,
  useCreateWarehouseMutation,
  useGetInventoryItemsQuery,
  useGetPurchaseOrdersQuery,
  useGetStoreOrdersQuery,
  useGetWarehousesQuery,
  useReceivePurchaseOrderMutation,
} from '@/app/api/erpApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type InvTab = 'items' | 'warehouses' | 'po' | 'store'

export function InventoryWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<InvTab>('items')
  const [feedback, setFeedback] = useState<string | null>(null)

  const [sku, setSku] = useState('')
  const [itemName, setItemName] = useState('')
  const [unitCost, setUnitCost] = useState('50')
  const [sellPrice, setSellPrice] = useState('100')
  const [initQty, setInitQty] = useState('25')

  const [whCode, setWhCode] = useState('')
  const [whName, setWhName] = useState('')
  const [whCity, setWhCity] = useState('')

  const [poVendor, setPoVendor] = useState('')
  const [poItemId, setPoItemId] = useState('')
  const [poWhId, setPoWhId] = useState('')
  const [poQty, setPoQty] = useState('10')

  const [custName, setCustName] = useState('')
  const [custEmail, setCustEmail] = useState('')
  const [ordItemId, setOrdItemId] = useState('')
  const [ordQty, setOrdQty] = useState('1')

  const skip = !accessToken || !user

  const { data: items = [] } = useGetInventoryItemsQuery(undefined, { skip })
  const { data: warehouses = [] } = useGetWarehousesQuery(undefined, { skip })
  const { data: pos = [] } = useGetPurchaseOrdersQuery(undefined, { skip })
  const { data: storeOrders = [] } = useGetStoreOrdersQuery(undefined, { skip })

  const [createItem] = useCreateInventoryItemMutation()
  const [createWarehouse] = useCreateWarehouseMutation()
  const [createPo] = useCreatePurchaseOrderMutation()
  const [receivePo] = useReceivePurchaseOrderMutation()
  const [checkoutOrder] = useCheckoutStoreOrderMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Inventory & Commerce</CardTitle>
          <CardDescription>Manage SKUs, warehouses, purchase orders, and online store checkout.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Inventory & E-Commerce</h1>
        <p className="text-sm text-muted-foreground">
          Multi-warehouse stock ledger, purchase order receiving, and online store order fulfillment
        </p>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        <Button size="sm" variant={tab === 'items' ? 'default' : 'outline'} onClick={() => setTab('items')}>
          <Boxes className="h-3.5 w-3.5" />
          <span>Items ({items.length})</span>
        </Button>
        <Button size="sm" variant={tab === 'warehouses' ? 'default' : 'outline'} onClick={() => setTab('warehouses')}>
          <Warehouse className="h-3.5 w-3.5" />
          <span>Warehouses ({warehouses.length})</span>
        </Button>
        <Button size="sm" variant={tab === 'po' ? 'default' : 'outline'} onClick={() => setTab('po')}>
          <Truck className="h-3.5 w-3.5" />
          <span>Purchase Orders ({pos.length})</span>
        </Button>
        <Button size="sm" variant={tab === 'store' ? 'default' : 'outline'} onClick={() => setTab('store')}>
          <ShoppingBag className="h-3.5 w-3.5" />
          <span>Online Store Orders ({storeOrders.length})</span>
        </Button>
      </div>

      {tab === 'items' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>SKU Catalog & Stock Levels</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5">
              {items.map((item) => (
                <div key={item.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold">{item.sku}</span>
                      <span className="text-sm font-semibold">{item.name}</span>
                      {item.lowStock && <Badge variant="warning">Low Stock</Badge>}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Cost: ${item.unitCost} · Price: ${item.sellingPrice}
                    </p>
                  </div>
                  <Badge variant="info">{item.totalQuantityOnHand} in stock</Badge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Add Inventory Item</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createItem({
                    sku,
                    name: itemName,
                    unitCost: Number(unitCost),
                    sellingPrice: Number(sellPrice),
                    initialQuantity: Number(initQty),
                  }).unwrap()
                  setSku('')
                  setItemName('')
                  setFeedback('Inventory item added.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="SKU" value={sku} onChange={(e) => setSku(e.target.value)} required />
                <Input placeholder="Item Name" value={itemName} onChange={(e) => setItemName(e.target.value)} required />
                <Input type="number" placeholder="Unit Cost" value={unitCost} onChange={(e) => setUnitCost(e.target.value)} required />
                <Input type="number" placeholder="Selling Price" value={sellPrice} onChange={(e) => setSellPrice(e.target.value)} required />
                <Input type="number" placeholder="Opening Stock" value={initQty} onChange={(e) => setInitQty(e.target.value)} />
                <Button type="submit" className="w-full">Save Item</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'warehouses' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Fulfillment Warehouses</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {warehouses.map((w) => (
                <div key={w.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <span className="font-mono font-bold mr-2">{w.code}</span>
                    <span className="text-sm font-semibold">{w.name}</span>
                    {w.city && <span className="ml-2 text-muted-foreground">({w.city})</span>}
                  </div>
                  {w.defaultWarehouse && <Badge variant="info">DEFAULT</Badge>}
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Add Warehouse</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createWarehouse({ code: whCode, name: whName, city: whCity || undefined }).unwrap()
                  setWhCode('')
                  setWhName('')
                  setWhCity('')
                  setFeedback('Warehouse created.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Code (e.g. BLR)" value={whCode} onChange={(e) => setWhCode(e.target.value)} required />
                <Input placeholder="Warehouse Name" value={whName} onChange={(e) => setWhName(e.target.value)} required />
                <Input placeholder="City" value={whCity} onChange={(e) => setWhCity(e.target.value)} />
                <Button type="submit" className="w-full">Create Warehouse</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'po' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Purchase Orders</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {pos.map((po) => (
                <div key={po.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <p className="font-semibold">
                      <span className="font-mono mr-2">{po.poNumber}</span>
                      {po.vendorName} — Qty {po.quantity} (${po.totalCost})
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={po.status === 'RECEIVED' ? 'success' : 'secondary'}>{po.status}</Badge>
                    {po.status === 'ORDERED' && (
                      <Button size="sm" variant="outline" onClick={() => receivePo(po.id)}>
                        Receive Stock
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Purchase Order</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  const itemId = poItemId || items[0]?.id
                  const warehouseId = poWhId || warehouses[0]?.id
                  if (!itemId || !warehouseId) return
                  await createPo({
                    vendorName: poVendor,
                    itemId,
                    warehouseId,
                    quantity: Number(poQty),
                    unitCost: 50,
                  }).unwrap()
                  setPoVendor('')
                  setFeedback('Purchase order created.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Vendor Name" value={poVendor} onChange={(e) => setPoVendor(e.target.value)} required />
                <select value={poItemId} onChange={(e) => setPoItemId(e.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm">
                  {items.map((i) => (
                    <option key={i.id} value={i.id}>{i.name}</option>
                  ))}
                </select>
                <select value={poWhId} onChange={(e) => setPoWhId(e.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm">
                  {warehouses.map((w) => (
                    <option key={w.id} value={w.id}>{w.name}</option>
                  ))}
                </select>
                <Input type="number" placeholder="Quantity" value={poQty} onChange={(e) => setPoQty(e.target.value)} required />
                <Button type="submit" className="w-full">Place PO</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'store' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Online Store Orders</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {storeOrders.map((o) => (
                <div key={o.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <p className="font-semibold">
                      <span className="font-mono mr-2">{o.orderNumber}</span>
                      {o.customerName} ({o.customerEmail})
                    </p>
                    <p className="text-muted-foreground">Qty: {o.quantity} · Total: ${o.totalAmount}</p>
                  </div>
                  <Badge variant="success">{o.status}</Badge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Quick Storefront Checkout</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  const itemId = ordItemId || items[0]?.id
                  if (!itemId) return
                  await checkoutOrder({
                    customerName: custName,
                    customerEmail: custEmail,
                    itemId,
                    quantity: Number(ordQty),
                  }).unwrap()
                  setCustName('')
                  setCustEmail('')
                  setFeedback('Order placed and warehouse stock deducted.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Customer Name" value={custName} onChange={(e) => setCustName(e.target.value)} required />
                <Input type="email" placeholder="Customer Email" value={custEmail} onChange={(e) => setCustEmail(e.target.value)} required />
                <select value={ordItemId} onChange={(e) => setOrdItemId(e.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm">
                  {items.map((i) => (
                    <option key={i.id} value={i.id}>{i.name} (${i.sellingPrice})</option>
                  ))}
                </select>
                <Input type="number" placeholder="Qty" value={ordQty} onChange={(e) => setOrdQty(e.target.value)} required />
                <Button type="submit" className="w-full">Checkout Order</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

