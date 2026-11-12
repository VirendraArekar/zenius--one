import { createApi, type BaseQueryFn } from '@reduxjs/toolkit/query/react'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { axiosClient } from './axiosClient'
import type { ApiResponse } from './systemApi'

interface AxiosBaseQueryArgs {
  url: string
  method?: AxiosRequestConfig['method']
  data?: AxiosRequestConfig['data']
  params?: AxiosRequestConfig['params']
}

interface AxiosBaseQueryError {
  status?: number
  data?: unknown
}

const axiosBaseQuery =
  (): BaseQueryFn<AxiosBaseQueryArgs, unknown, AxiosBaseQueryError> =>
  async ({ url, method = 'GET', data, params }) => {
    try {
      const result = await axiosClient.request({ url, method, data, params })
      return { data: result.data }
    } catch (err) {
      const error = err as AxiosError
      return {
        error: {
          status: error.response?.status,
          data: error.response?.data || error.message,
        },
      }
    }
  }

export interface FinAccountDto {
  id: string
  code: string
  name: string
  accountType: string
  currency: string
  systemAccount: boolean
}

export interface InvoiceDto {
  id: string
  invoiceNumber: string
  customerName: string
  subtotal: number
  gstRate: number
  gstAmount: number
  totalAmount: number
  balanceDue: number
  status: string
  dueDate: string
}

export interface BillDto {
  id: string
  billNumber: string
  vendorName: string
  subtotal: number
  tdsAmount: number
  totalAmount: number
  balanceDue: number
  status: string
  dueDate: string
}

export interface FinancialReportsDto {
  totalDebits: number
  totalCredits: number
  totalRevenue: number
  totalExpense: number
  netProfit: number
  totalAssets: number
  totalLiabilities: number
  arOutstanding: number
  apOutstanding: number
  totalGstCollected: number
  totalTdsDeducted: number
  trialBalance: {
    accountId: string
    code: string
    name: string
    accountType: string
    debit: number
    credit: number
    netBalance: number
  }[]
}

export interface PlanDto {
  id: string
  code: string
  name: string
  billingInterval: string
  price: number
  currency: string
  trialDays: number
}

export interface SubscriptionDto {
  id: string
  subscriptionNumber: string
  planId: string
  planName: string
  planPrice: number
  customerName: string
  customerEmail: string
  status: string
  currentPeriodEnd: string
  creditBalance: number
}

export interface WarehouseDto {
  id: string
  code: string
  name: string
  city: string | null
  defaultWarehouse: boolean
}

export interface ItemWithStockDto {
  id: string
  sku: string
  name: string
  category: string | null
  unitCost: number
  sellingPrice: number
  reorderLevel: number
  totalQuantityOnHand: number
  lowStock: boolean
}

export interface PurchaseOrderDto {
  id: string
  poNumber: string
  vendorName: string
  warehouseId: string
  itemId: string
  quantity: number
  totalCost: number
  status: string
}

export interface StoreOrderDto {
  id: string
  orderNumber: string
  customerName: string
  customerEmail: string
  quantity: number
  totalAmount: number
  status: string
}

export const erpApi = createApi({
  reducerPath: 'erpApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['FinAccounts', 'FinInvoices', 'FinBills', 'FinReports', 'Plans', 'Subscriptions', 'Warehouses', 'Items', 'POs', 'StoreOrders'],
  endpoints: (builder) => ({
    getFinAccounts: builder.query<FinAccountDto[], void>({
      query: () => ({ url: '/finance/accounts' }),
      transformResponse: (res: ApiResponse<FinAccountDto[]>) => res.data,
      providesTags: ['FinAccounts'],
    }),
    getFinInvoices: builder.query<ApiResponse<InvoiceDto[]>, void>({
      query: () => ({ url: '/finance/invoices', params: { page: 0, size: 50 } }),
      providesTags: ['FinInvoices'],
    }),
    createFinInvoice: builder.mutation<InvoiceDto, { customerName: string; subtotal: number; gstRate: number }>({
      query: (data) => ({ url: '/finance/invoices', method: 'POST', data }),
      transformResponse: (res: ApiResponse<InvoiceDto>) => res.data,
      invalidatesTags: ['FinInvoices', 'FinReports'],
    }),
    recordInvoicePayment: builder.mutation<InvoiceDto, { id: string; amount: number }>({
      query: ({ id, amount }) => ({ url: `/finance/invoices/${id}/payments`, method: 'POST', data: { amount } }),
      transformResponse: (res: ApiResponse<InvoiceDto>) => res.data,
      invalidatesTags: ['FinInvoices', 'FinReports'],
    }),
    getFinBills: builder.query<ApiResponse<BillDto[]>, void>({
      query: () => ({ url: '/finance/bills', params: { page: 0, size: 50 } }),
      providesTags: ['FinBills'],
    }),
    createFinBill: builder.mutation<BillDto, { vendorName: string; subtotal: number; tdsRate: number }>({
      query: (data) => ({ url: '/finance/bills', method: 'POST', data }),
      transformResponse: (res: ApiResponse<BillDto>) => res.data,
      invalidatesTags: ['FinBills', 'FinReports'],
    }),
    payFinBill: builder.mutation<BillDto, { id: string; amount: number }>({
      query: ({ id, amount }) => ({ url: `/finance/bills/${id}/payments`, method: 'POST', data: { amount } }),
      transformResponse: (res: ApiResponse<BillDto>) => res.data,
      invalidatesTags: ['FinBills', 'FinReports'],
    }),
    getFinancialReports: builder.query<FinancialReportsDto, void>({
      query: () => ({ url: '/finance/reports' }),
      transformResponse: (res: ApiResponse<FinancialReportsDto>) => res.data,
      providesTags: ['FinReports'],
    }),

    // Billing
    getPlans: builder.query<PlanDto[], void>({
      query: () => ({ url: '/billing/plans' }),
      transformResponse: (res: ApiResponse<PlanDto[]>) => res.data,
      providesTags: ['Plans'],
    }),
    createPlan: builder.mutation<PlanDto, { code: string; name: string; price: number; trialDays: number }>({
      query: (data) => ({ url: '/billing/plans', method: 'POST', data }),
      transformResponse: (res: ApiResponse<PlanDto>) => res.data,
      invalidatesTags: ['Plans'],
    }),
    getSubscriptions: builder.query<SubscriptionDto[], void>({
      query: () => ({ url: '/billing/subscriptions' }),
      transformResponse: (res: ApiResponse<SubscriptionDto[]>) => res.data,
      providesTags: ['Subscriptions'],
    }),
    createSubscription: builder.mutation<SubscriptionDto, { planId: string; customerName: string; customerEmail: string }>({
      query: (data) => ({ url: '/billing/subscriptions', method: 'POST', data }),
      transformResponse: (res: ApiResponse<SubscriptionDto>) => res.data,
      invalidatesTags: ['Subscriptions'],
    }),
    cancelSubscription: builder.mutation<SubscriptionDto, string>({
      query: (id) => ({ url: `/billing/subscriptions/${id}/cancel`, method: 'POST' }),
      transformResponse: (res: ApiResponse<SubscriptionDto>) => res.data,
      invalidatesTags: ['Subscriptions'],
    }),

    // Inventory
    getWarehouses: builder.query<WarehouseDto[], void>({
      query: () => ({ url: '/inventory/warehouses' }),
      transformResponse: (res: ApiResponse<WarehouseDto[]>) => res.data,
      providesTags: ['Warehouses'],
    }),
    createWarehouse: builder.mutation<WarehouseDto, { code: string; name: string; city?: string }>({
      query: (data) => ({ url: '/inventory/warehouses', method: 'POST', data }),
      transformResponse: (res: ApiResponse<WarehouseDto>) => res.data,
      invalidatesTags: ['Warehouses'],
    }),
    getInventoryItems: builder.query<ItemWithStockDto[], void>({
      query: () => ({ url: '/inventory/items' }),
      transformResponse: (res: ApiResponse<ItemWithStockDto[]>) => res.data,
      providesTags: ['Items'],
    }),
    createInventoryItem: builder.mutation<
      ItemWithStockDto,
      { sku: string; name: string; unitCost: number; sellingPrice: number; initialQuantity: number }
    >({
      query: (data) => ({ url: '/inventory/items', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ItemWithStockDto>) => res.data,
      invalidatesTags: ['Items'],
    }),
    transferStock: builder.mutation<void, { itemId: string; fromWarehouseId: string; toWarehouseId: string; quantity: number }>({
      query: (data) => ({ url: '/inventory/transfers', method: 'POST', data }),
      invalidatesTags: ['Items'],
    }),
    getPurchaseOrders: builder.query<PurchaseOrderDto[], void>({
      query: () => ({ url: '/inventory/purchase-orders' }),
      transformResponse: (res: ApiResponse<PurchaseOrderDto[]>) => res.data,
      providesTags: ['POs'],
    }),
    createPurchaseOrder: builder.mutation<
      PurchaseOrderDto,
      { vendorName: string; warehouseId: string; itemId: string; quantity: number; unitCost: number }
    >({
      query: (data) => ({ url: '/inventory/purchase-orders', method: 'POST', data }),
      transformResponse: (res: ApiResponse<PurchaseOrderDto>) => res.data,
      invalidatesTags: ['POs'],
    }),
    receivePurchaseOrder: builder.mutation<PurchaseOrderDto, string>({
      query: (id) => ({ url: `/inventory/purchase-orders/${id}/receive`, method: 'POST' }),
      transformResponse: (res: ApiResponse<PurchaseOrderDto>) => res.data,
      invalidatesTags: ['POs', 'Items'],
    }),
    getStoreOrders: builder.query<StoreOrderDto[], void>({
      query: () => ({ url: '/inventory/store-orders' }),
      transformResponse: (res: ApiResponse<StoreOrderDto[]>) => res.data,
      providesTags: ['StoreOrders'],
    }),
    checkoutStoreOrder: builder.mutation<
      StoreOrderDto,
      { customerName: string; customerEmail: string; itemId: string; quantity: number }
    >({
      query: (data) => ({ url: '/inventory/store-orders', method: 'POST', data }),
      transformResponse: (res: ApiResponse<StoreOrderDto>) => res.data,
      invalidatesTags: ['StoreOrders', 'Items'],
    }),
  }),
})

export const {
  useGetFinAccountsQuery,
  useGetFinInvoicesQuery,
  useCreateFinInvoiceMutation,
  useRecordInvoicePaymentMutation,
  useGetFinBillsQuery,
  useCreateFinBillMutation,
  usePayFinBillMutation,
  useGetFinancialReportsQuery,
  useGetPlansQuery,
  useCreatePlanMutation,
  useGetSubscriptionsQuery,
  useCreateSubscriptionMutation,
  useCancelSubscriptionMutation,
  useGetWarehousesQuery,
  useCreateWarehouseMutation,
  useGetInventoryItemsQuery,
  useCreateInventoryItemMutation,
  useTransferStockMutation,
  useGetPurchaseOrdersQuery,
  useCreatePurchaseOrderMutation,
  useReceivePurchaseOrderMutation,
  useGetStoreOrdersQuery,
  useCheckoutStoreOrderMutation,
} = erpApi

