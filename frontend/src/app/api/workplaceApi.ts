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

export interface ProjectDto {
  id: string
  projectCode: string
  name: string
  clientName: string | null
  status: string
  budgetAmount: number
  startDate: string | null
  dueDate: string | null
}

export interface SprintDto {
  id: string
  projectId: string
  name: string
  goal: string | null
  startDate: string | null
  endDate: string | null
  status: string
}

export interface TaskDto {
  id: string
  projectId: string
  sprintId: string | null
  taskKey: string
  title: string
  taskType: string
  priority: string
  status: string
  storyPoints: number | null
  assigneeName: string | null
  dueDate: string | null
  loggedMinutes: number
}

export interface TimeLogDto {
  id: string
  projectId: string
  taskId: string
  memberName: string
  minutesLogged: number
  billable: boolean
  workDate: string
  notes: string | null
}

export interface MailDto {
  id: string
  folder: string
  senderEmail: string
  recipientEmail: string
  subject: string
  body: string
  labels: string | null
  read: boolean
  starred: boolean
  createdAt: string
}

export interface ChannelDto {
  id: string
  name: string
  description: string | null
  privateChannel: boolean
}

export interface MessageDto {
  id: string
  channelId: string
  senderName: string
  body: string
  createdAt: string
}

export interface MeetingDto {
  id: string
  title: string
  organizerName: string
  scheduledAt: string
  durationMinutes: number
  meetingLink: string
}

export interface FolderDto {
  id: string
  parentFolderId: string | null
  name: string
  shared: boolean
  createdAt: string
}

export interface FileDto {
  id: string
  folderId: string | null
  title: string
  docType: string
  currentVersion: number
  retentionPolicy: string | null
  updatedAt: string
}

export interface RevisionDto {
  id: string
  revisionNumber: number
  editorName: string
  contentSnapshot: string
  createdAt: string
}

export interface SignatureRequestDto {
  id: string
  documentId: string
  signerName: string
  signerEmail: string
  status: string
  signatureHash: string | null
  signedAt: string | null
  createdAt: string
}

export interface FileDetailDto {
  file: FileDto
  contentBody: string
  revisions: RevisionDto[]
  signatures: SignatureRequestDto[]
}

export interface ContractDto {
  id: string
  contractNumber: string
  title: string
  counterpartyName: string
  contractType: string
  valueAmount: number
  effectiveDate: string
  expirationDate: string
  status: string
  documentId: string | null
  createdAt: string
}

export const workplaceApi = createApi({
  reducerPath: 'workplaceApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Projects', 'Sprints', 'Tasks', 'TimeLogs', 'Mail', 'Channels', 'Messages', 'Meetings', 'Folders', 'Files', 'Signatures', 'Contracts'],
  endpoints: (builder) => ({
    getProjects: builder.query<ProjectDto[], void>({
      query: () => ({ url: '/projects' }),
      transformResponse: (res: ApiResponse<ProjectDto[]>) => res.data,
      providesTags: ['Projects'],
    }),
    createProject: builder.mutation<ProjectDto, { name: string; clientName?: string; budgetAmount?: number; startDate?: string; dueDate?: string }>({
      query: (data) => ({ url: '/projects', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ProjectDto>) => res.data,
      invalidatesTags: ['Projects'],
    }),
    getSprints: builder.query<SprintDto[], string>({
      query: (projectId) => ({ url: `/projects/${projectId}/sprints` }),
      transformResponse: (res: ApiResponse<SprintDto[]>) => res.data,
      providesTags: ['Sprints'],
    }),
    createSprint: builder.mutation<SprintDto, { projectId: string; name: string; goal?: string; startDate?: string; endDate?: string }>({
      query: (data) => ({ url: '/projects/sprints', method: 'POST', data }),
      transformResponse: (res: ApiResponse<SprintDto>) => res.data,
      invalidatesTags: ['Sprints'],
    }),
    getTasks: builder.query<TaskDto[], string | void>({
      query: (projectId) => ({ url: '/projects/tasks', params: projectId ? { projectId } : undefined }),
      transformResponse: (res: ApiResponse<TaskDto[]>) => res.data,
      providesTags: ['Tasks'],
    }),
    createTask: builder.mutation<TaskDto, { projectId: string; sprintId?: string; title: string; taskType?: string; priority?: string; storyPoints?: number; assigneeName?: string; dueDate?: string }>({
      query: (data) => ({ url: '/projects/tasks', method: 'POST', data }),
      transformResponse: (res: ApiResponse<TaskDto>) => res.data,
      invalidatesTags: ['Tasks'],
    }),
    updateTaskStatus: builder.mutation<TaskDto, { id: string; status: string }>({
      query: ({ id, status }) => ({ url: `/projects/tasks/${id}/status`, method: 'PATCH', data: { status } }),
      transformResponse: (res: ApiResponse<TaskDto>) => res.data,
      invalidatesTags: ['Tasks'],
    }),
    getTimeLogs: builder.query<TimeLogDto[], void>({
      query: () => ({ url: '/projects/time-logs' }),
      transformResponse: (res: ApiResponse<TimeLogDto[]>) => res.data,
      providesTags: ['TimeLogs'],
    }),
    logTime: builder.mutation<TimeLogDto, { taskId: string; minutesLogged: number; billable: boolean; workDate?: string; notes?: string }>({
      query: (data) => ({ url: '/projects/time-logs', method: 'POST', data }),
      transformResponse: (res: ApiResponse<TimeLogDto>) => res.data,
      invalidatesTags: ['TimeLogs', 'Tasks'],
    }),

    getMail: builder.query<MailDto[], string | void>({
      query: (folder) => ({ url: '/collab/mail', params: folder ? { folder } : undefined }),
      transformResponse: (res: ApiResponse<MailDto[]>) => res.data,
      providesTags: ['Mail'],
    }),
    sendMail: builder.mutation<MailDto, { recipientEmail: string; subject: string; body: string; labels?: string }>({
      query: (data) => ({ url: '/collab/mail', method: 'POST', data }),
      transformResponse: (res: ApiResponse<MailDto>) => res.data,
      invalidatesTags: ['Mail'],
    }),
    markMailRead: builder.mutation<MailDto, { id: string; read: boolean }>({
      query: ({ id, read }) => ({ url: `/collab/mail/${id}/read`, method: 'PATCH', data: { read } }),
      transformResponse: (res: ApiResponse<MailDto>) => res.data,
      invalidatesTags: ['Mail'],
    }),
    toggleMailStar: builder.mutation<MailDto, string>({
      query: (id) => ({ url: `/collab/mail/${id}/star`, method: 'PATCH' }),
      transformResponse: (res: ApiResponse<MailDto>) => res.data,
      invalidatesTags: ['Mail'],
    }),
    replyMail: builder.mutation<MailDto, { id: string; body: string }>({
      query: ({ id, body }) => ({ url: `/collab/mail/${id}/reply`, method: 'POST', data: { body } }),
      transformResponse: (res: ApiResponse<MailDto>) => res.data,
      invalidatesTags: ['Mail'],
    }),
    getChannels: builder.query<ChannelDto[], void>({
      query: () => ({ url: '/collab/channels' }),
      transformResponse: (res: ApiResponse<ChannelDto[]>) => res.data,
      providesTags: ['Channels'],
    }),
    createChannel: builder.mutation<ChannelDto, { name: string; description?: string; privateChannel: boolean }>({
      query: (data) => ({ url: '/collab/channels', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ChannelDto>) => res.data,
      invalidatesTags: ['Channels'],
    }),
    getChannelMessages: builder.query<MessageDto[], string>({
      query: (channelId) => ({ url: `/collab/channels/${channelId}/messages` }),
      transformResponse: (res: ApiResponse<MessageDto[]>) => res.data,
      providesTags: ['Messages'],
    }),
    postMessage: builder.mutation<MessageDto, { channelId: string; body: string }>({
      query: ({ channelId, body }) => ({ url: `/collab/channels/${channelId}/messages`, method: 'POST', data: { body } }),
      transformResponse: (res: ApiResponse<MessageDto>) => res.data,
      invalidatesTags: ['Messages'],
    }),
    getMeetings: builder.query<MeetingDto[], void>({
      query: () => ({ url: '/collab/meetings' }),
      transformResponse: (res: ApiResponse<MeetingDto[]>) => res.data,
      providesTags: ['Meetings'],
    }),
    scheduleMeeting: builder.mutation<MeetingDto, { title: string; scheduledAt?: string; durationMinutes?: number }>({
      query: (data) => ({ url: '/collab/meetings', method: 'POST', data }),
      transformResponse: (res: ApiResponse<MeetingDto>) => res.data,
      invalidatesTags: ['Meetings'],
    }),

    getFolders: builder.query<FolderDto[], void>({
      query: () => ({ url: '/docs/folders' }),
      transformResponse: (res: ApiResponse<FolderDto[]>) => res.data,
      providesTags: ['Folders'],
    }),
    createFolder: builder.mutation<FolderDto, { name: string; parentFolderId?: string; shared?: boolean }>({
      query: (data) => ({ url: '/docs/folders', method: 'POST', data }),
      transformResponse: (res: ApiResponse<FolderDto>) => res.data,
      invalidatesTags: ['Folders'],
    }),
    getFiles: builder.query<FileDto[], string | void>({
      query: (folderId) => ({ url: '/docs/files', params: folderId ? { folderId } : undefined }),
      transformResponse: (res: ApiResponse<FileDto[]>) => res.data,
      providesTags: ['Files'],
    }),
    getFileDetail: builder.query<FileDetailDto, string>({
      query: (id) => ({ url: `/docs/files/${id}` }),
      transformResponse: (res: ApiResponse<FileDetailDto>) => res.data,
      providesTags: ['Files', 'Signatures'],
    }),
    createFile: builder.mutation<FileDetailDto, { folderId?: string; title: string; docType?: string; contentBody?: string; retentionPolicy?: string }>({
      query: (data) => ({ url: '/docs/files', method: 'POST', data }),
      transformResponse: (res: ApiResponse<FileDetailDto>) => res.data,
      invalidatesTags: ['Files'],
    }),
    updateFile: builder.mutation<FileDetailDto, { id: string; title?: string; contentBody?: string; retentionPolicy?: string }>({
      query: ({ id, ...data }) => ({ url: `/docs/files/${id}`, method: 'PATCH', data }),
      transformResponse: (res: ApiResponse<FileDetailDto>) => res.data,
      invalidatesTags: ['Files'],
    }),
    getSignatures: builder.query<SignatureRequestDto[], string | void>({
      query: (documentId) => ({ url: '/docs/signatures', params: documentId ? { documentId } : undefined }),
      transformResponse: (res: ApiResponse<SignatureRequestDto[]>) => res.data,
      providesTags: ['Signatures'],
    }),
    requestSignature: builder.mutation<SignatureRequestDto, { fileId: string; signerName: string; signerEmail: string }>({
      query: ({ fileId, ...data }) => ({ url: `/docs/files/${fileId}/signature-requests`, method: 'POST', data }),
      transformResponse: (res: ApiResponse<SignatureRequestDto>) => res.data,
      invalidatesTags: ['Signatures', 'Files'],
    }),
    signDocument: builder.mutation<SignatureRequestDto, string>({
      query: (signatureId) => ({ url: `/docs/signatures/${signatureId}/sign`, method: 'POST' }),
      transformResponse: (res: ApiResponse<SignatureRequestDto>) => res.data,
      invalidatesTags: ['Signatures', 'Files'],
    }),

    getContracts: builder.query<ContractDto[], void>({
      query: () => ({ url: '/legal/contracts' }),
      transformResponse: (res: ApiResponse<ContractDto[]>) => res.data,
      providesTags: ['Contracts'],
    }),
    createContract: builder.mutation<ContractDto, { title: string; counterpartyName: string; contractType: string; valueAmount: number; effectiveDate: string; expirationDate: string; documentId?: string }>({
      query: (data) => ({ url: '/legal/contracts', method: 'POST', data }),
      transformResponse: (res: ApiResponse<ContractDto>) => res.data,
      invalidatesTags: ['Contracts'],
    }),
    updateContractStatus: builder.mutation<ContractDto, { id: string; status: string }>({
      query: ({ id, status }) => ({ url: `/legal/contracts/${id}/status`, method: 'PATCH', data: { status } }),
      transformResponse: (res: ApiResponse<ContractDto>) => res.data,
      invalidatesTags: ['Contracts'],
    }),
  }),
})

export const {
  useGetProjectsQuery,
  useCreateProjectMutation,
  useGetSprintsQuery,
  useCreateSprintMutation,
  useGetTasksQuery,
  useCreateTaskMutation,
  useUpdateTaskStatusMutation,
  useGetTimeLogsQuery,
  useLogTimeMutation,
  useGetMailQuery,
  useSendMailMutation,
  useMarkMailReadMutation,
  useToggleMailStarMutation,
  useReplyMailMutation,
  useGetChannelsQuery,
  useCreateChannelMutation,
  useGetChannelMessagesQuery,
  usePostMessageMutation,
  useGetMeetingsQuery,
  useScheduleMeetingMutation,
  useGetFoldersQuery,
  useCreateFolderMutation,
  useGetFilesQuery,
  useGetFileDetailQuery,
  useCreateFileMutation,
  useUpdateFileMutation,
  useGetSignaturesQuery,
  useRequestSignatureMutation,
  useSignDocumentMutation,
  useGetContractsQuery,
  useCreateContractMutation,
  useUpdateContractStatusMutation,
} = workplaceApi

