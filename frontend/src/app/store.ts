import { configureStore } from '@reduxjs/toolkit'
import uiReducer from './slices/uiSlice'
import authReducer from './slices/authSlice'
import { systemApi } from './api/systemApi'
import { adminApi } from './api/adminApi'
import { crmApi } from './api/crmApi'
import { growthAndSupportApi } from './api/growthAndSupportApi'
import { erpApi } from './api/erpApi'
import { peopleApi } from './api/peopleApi'
import { workplaceApi } from './api/workplaceApi'
import { intelligenceApi } from './api/intelligenceApi'
import { aiApi } from './api/aiApi'

export const store = configureStore({
  reducer: {
    ui: uiReducer,
    auth: authReducer,
    [systemApi.reducerPath]: systemApi.reducer,
    [adminApi.reducerPath]: adminApi.reducer,
    [crmApi.reducerPath]: crmApi.reducer,
    [growthAndSupportApi.reducerPath]: growthAndSupportApi.reducer,
    [erpApi.reducerPath]: erpApi.reducer,
    [peopleApi.reducerPath]: peopleApi.reducer,
    [workplaceApi.reducerPath]: workplaceApi.reducer,
    [intelligenceApi.reducerPath]: intelligenceApi.reducer,
    [aiApi.reducerPath]: aiApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      systemApi.middleware,
      adminApi.middleware,
      crmApi.middleware,
      growthAndSupportApi.middleware,
      erpApi.middleware,
      peopleApi.middleware,
      workplaceApi.middleware,
      intelligenceApi.middleware,
      aiApi.middleware
    ),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
