export const roleHomes: Record<string, string> = {
  SCHOOL_ADMIN: "/admin-school",
  TEACHER: "/teacher",
  PARENT: "/parent",
  STUDENT: "/student",
};
export const roleNames: Record<string, string> = {
  SCHOOL_ADMIN: "Quản trị nhà trường",
  TEACHER: "Giáo viên",
  PARENT: "Phụ huynh",
  STUDENT: "Học sinh",
};
export function home(roles: string[]) {
  return (
    Object.keys(roleHomes)
      .map((role) => (roles.includes(role) ? roleHomes[role] : null))
      .find(Boolean) || "/403"
  );
}
export function safeReturn(value: unknown, roles: string[]) {
  if (
    typeof value !== "string" ||
    !value.startsWith("/") ||
    value.startsWith("//") ||
    [...value].some((c) => c === "\\" || c.charCodeAt(0) <= 32)
  )
    return home(roles);
  const path = value.split(/[?#]/)[0];
  const role = Object.keys(roleHomes).find(
    (r) => path === roleHomes[r] || path.startsWith(roleHomes[r] + "/"),
  );
  return role && roles.includes(role) ? value : home(roles);
}
