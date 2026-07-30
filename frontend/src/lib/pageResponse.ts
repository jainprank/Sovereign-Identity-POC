/**
 * Mirrors backend common.web.PageResponse<T> - the envelope every paginated list endpoint
 * (DIDs, credentials, credential schemas, audit records, API request logs, etc.) returns,
 * as opposed to a bare array.
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
