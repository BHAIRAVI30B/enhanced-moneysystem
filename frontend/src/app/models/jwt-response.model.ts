export interface JwtResponse {
    accessToken: string;
    tokenType: string;
    id: number;
    username: string;
    roles: string[];
}