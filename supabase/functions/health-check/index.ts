import "https://deno.land/x/xhr@0.1.0/mod.ts";
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

console.log("Health check function initialized");

serve(async (req) => {
  const data = {
    status: "online",
    message: "Edge Function is operational",
    version: "1.0"
  };

  return new Response(
    JSON.stringify(data),
    { headers: { "Content-Type": "application/json" } },
  );
});
