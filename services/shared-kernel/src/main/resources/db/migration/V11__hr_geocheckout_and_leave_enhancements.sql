-- Add check-out GPS coordinates and resolved location label to HR attendance records
ALTER TABLE hr_attendance_records ADD COLUMN IF NOT EXISTS check_out_latitude DOUBLE PRECISION;
ALTER TABLE hr_attendance_records ADD COLUMN IF NOT EXISTS check_out_longitude DOUBLE PRECISION;
ALTER TABLE hr_attendance_records ADD COLUMN IF NOT EXISTS location_label VARCHAR(160);

