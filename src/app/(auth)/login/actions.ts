"use server";

import { postApiAuthLogin } from "@/types";
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from "@/middleware";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export async function LoginAction(email: string, password: string)
{
    const result = await postApiAuthLogin({
        body: { email, password },
    });

    if (result.error !== undefined)
    {
        if (result.response.status === 401) return { error: "Credenciales incorrectos" };
        return { error: "Error iniciando sesión" };
    }

    const cookieStore = await cookies();
    const data = result.data!;
    cookieStore.set({
        name: ACCESS_TOKEN_KEY,
        value: data.accessToken,
        httpOnly: true,
        expires: Date.now() + (data.accessExpiresIn * 1000),
        path: "/",
    });
    cookieStore.set({
        name: REFRESH_TOKEN_KEY,
        value: data.refreshToken,
        httpOnly: true,
        expires: Date.now() + (data.refreshExpiresIn * 1000),
        path: "/",
    });

    redirect("/");
}
