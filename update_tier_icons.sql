-- 更新段位图标 SVG

-- 铜牌
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="bronze-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#D4A574"/>
            <stop offset="50%" style="stop-color:#CD7F32"/>
            <stop offset="100%" style="stop-color:#8B5E3C"/>
        </linearGradient>
    </defs>
    <path d="M28 4 L48 14 L48 30 C48 42 38 50 28 54 C18 50 8 42 8 30 L8 14 Z" fill="url(#bronze-grad)" stroke="#8B5E3C" stroke-width="1.5"/>
    <path d="M28 8 L44 16 L44 30 C44 40 36 47 28 50 C20 47 12 40 12 30 L12 16 Z" fill="none" stroke="#D4A574" stroke-width="1" opacity="0.6"/>
    <polygon points="28,18 31,26 40,26 33,31 35,39 28,34 21,39 23,31 16,26 25,26" fill="#FFE0B2" opacity="0.8"/>
</svg>' WHERE sort_order = 1;

-- 银牌
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="silver-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#E8E8E8"/>
            <stop offset="50%" style="stop-color:#C0C0C0"/>
            <stop offset="100%" style="stop-color:#A0A0A0"/>
        </linearGradient>
    </defs>
    <path d="M10 28 C10 28 14 20 20 22 C16 24 14 28 14 28" fill="#D0D0D0" opacity="0.7"/>
    <path d="M8 32 C8 32 12 22 20 24 C14 26 12 32 12 32" fill="#B8B8B8" opacity="0.6"/>
    <path d="M46 28 C46 28 42 20 36 22 C40 24 42 28 42 28" fill="#D0D0D0" opacity="0.7"/>
    <path d="M48 32 C48 32 44 22 36 24 C42 26 44 32 44 32" fill="#B8B8B8" opacity="0.6"/>
    <path d="M28 6 L46 16 L46 30 C46 42 36 48 28 52 C20 48 10 42 10 30 L10 16 Z" fill="url(#silver-grad)" stroke="#909090" stroke-width="1.5"/>
    <path d="M28 10 L42 18 L42 30 C42 40 34 45 28 48 C22 45 14 40 14 30 L14 18 Z" fill="none" stroke="#E0E0E0" stroke-width="1" opacity="0.5"/>
    <polygon points="28,20 31,27 38,27 32,32 34,39 28,35 22,39 24,32 18,27 25,27" fill="white" opacity="0.9"/>
</svg>' WHERE sort_order = 2;

-- 金牌
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="gold-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#FFE082"/>
            <stop offset="40%" style="stop-color:#FFD700"/>
            <stop offset="100%" style="stop-color:#FFA000"/>
        </linearGradient>
    </defs>
    <line x1="28" y1="2" x2="28" y2="10" stroke="#FFD700" stroke-width="1.5" opacity="0.5"/>
    <line x1="10" y1="10" x2="16" y2="16" stroke="#FFD700" stroke-width="1.5" opacity="0.4"/>
    <line x1="46" y1="10" x2="40" y2="16" stroke="#FFD700" stroke-width="1.5" opacity="0.4"/>
    <line x1="4" y1="28" x2="12" y2="28" stroke="#FFD700" stroke-width="1.5" opacity="0.3"/>
    <line x1="52" y1="28" x2="44" y2="28" stroke="#FFD700" stroke-width="1.5" opacity="0.3"/>
    <path d="M8 26 C8 26 14 16 22 18 C16 20 12 26 12 26" fill="#FFE082" opacity="0.8"/>
    <path d="M6 30 C6 30 12 18 22 20 C14 22 10 30 10 30" fill="#FFD700" opacity="0.7"/>
    <path d="M48 26 C48 26 42 16 34 18 C40 20 44 26 44 26" fill="#FFE082" opacity="0.8"/>
    <path d="M50 30 C50 30 44 18 34 20 C42 22 46 30 46 30" fill="#FFD700" opacity="0.7"/>
    <path d="M28 8 L46 18 L46 32 C46 42 36 48 28 52 C20 48 10 42 10 32 L10 18 Z" fill="url(#gold-grad)" stroke="#E6A800" stroke-width="1.5"/>
    <path d="M28 12 L42 20 L42 32 C42 40 34 45 28 48 C22 45 14 40 14 32 L14 20 Z" fill="none" stroke="#FFF8E1" stroke-width="1" opacity="0.5"/>
    <path d="M20 28 L24 22 L28 26 L32 22 L36 28 L34 34 L22 34 Z" fill="#FFF8E1" opacity="0.9"/>
    <circle cx="24" cy="24" r="1.5" fill="#FFA000"/>
    <circle cx="28" cy="22" r="1.5" fill="#FFA000"/>
    <circle cx="32" cy="24" r="1.5" fill="#FFA000"/>
</svg>' WHERE sort_order = 3;

-- 蓝宝石
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="sapphire-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#81D4FA"/>
            <stop offset="50%" style="stop-color:#0288D1"/>
            <stop offset="100%" style="stop-color:#01579B"/>
        </linearGradient>
    </defs>
    <polygon points="28,4 48,16 52,28 48,40 28,52 8,40 4,28 8,16" fill="#4FC3F7" opacity="0.2"/>
    <polygon points="28,6 46,16 50,28 46,40 28,50 10,40 6,28 10,16" fill="url(#sapphire-grad)"/>
    <polygon points="28,6 28,28 46,16" fill="rgba(255,255,255,0.2)"/>
    <polygon points="28,6 28,28 10,16" fill="rgba(0,0,0,0.1)"/>
    <polygon points="50,28 28,28 46,16" fill="rgba(255,255,255,0.15)"/>
    <polygon points="50,28 28,28 46,40" fill="rgba(0,0,0,0.15)"/>
    <polygon points="28,50 28,28 46,40" fill="rgba(255,255,255,0.05)"/>
    <polygon points="28,50 28,28 10,40" fill="rgba(0,0,0,0.2)"/>
    <polygon points="6,28 28,28 10,40" fill="rgba(255,255,255,0.1)"/>
    <polygon points="6,28 28,28 10,16" fill="rgba(0,0,0,0.1)"/>
    <ellipse cx="22" cy="20" rx="8" ry="5" fill="white" opacity="0.3" transform="rotate(-20 22 20)"/>
</svg>' WHERE sort_order = 4;

-- 红宝石
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="ruby-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#FF8A80"/>
            <stop offset="40%" style="stop-color:#E53935"/>
            <stop offset="100%" style="stop-color:#B71C1C"/>
        </linearGradient>
        <radialGradient id="ruby-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" style="stop-color:#FF5252;stop-opacity:0.4"/>
            <stop offset="100%" style="stop-color:#FF5252;stop-opacity:0"/>
        </radialGradient>
    </defs>
    <circle cx="28" cy="28" r="24" fill="url(#ruby-glow)"/>
    <path d="M18 10 C18 10 20 6 22 10 C22 10 24 6 24 10" fill="#FF8A80" opacity="0.6"/>
    <path d="M32 8 C32 8 34 4 36 8 C36 8 38 4 38 8" fill="#FFAB91" opacity="0.5"/>
    <path d="M42 14 C42 14 44 10 46 14" fill="#FF8A80" opacity="0.4"/>
    <polygon points="28,6 46,16 50,28 46,40 28,50 10,40 6,28 10,16" fill="url(#ruby-grad)"/>
    <polygon points="28,6 28,28 46,16" fill="rgba(255,255,255,0.2)"/>
    <polygon points="28,6 28,28 10,16" fill="rgba(0,0,0,0.1)"/>
    <polygon points="50,28 28,28 46,16" fill="rgba(255,255,255,0.15)"/>
    <polygon points="50,28 28,28 46,40" fill="rgba(0,0,0,0.2)"/>
    <polygon points="28,50 28,28 46,40" fill="rgba(255,255,255,0.05)"/>
    <polygon points="28,50 28,28 10,40" fill="rgba(0,0,0,0.25)"/>
    <polygon points="28,16 30,24 38,24 32,28 34,36 28,32 22,36 24,28 18,24 26,24" fill="white" opacity="0.15"/>
    <ellipse cx="22" cy="20" rx="8" ry="5" fill="white" opacity="0.35" transform="rotate(-20 22 20)"/>
</svg>' WHERE sort_order = 5;

-- 紫水晶
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="amethyst-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#CE93D8"/>
            <stop offset="40%" style="stop-color:#9C27B0"/>
            <stop offset="100%" style="stop-color:#6A1B9A"/>
        </linearGradient>
        <radialGradient id="amethyst-glow" cx="50%" cy="40%" r="50%">
            <stop offset="0%" style="stop-color:#E1BEE7;stop-opacity:0.6"/>
            <stop offset="100%" style="stop-color:#E1BEE7;stop-opacity:0"/>
        </radialGradient>
    </defs>
    <circle cx="28" cy="24" r="22" fill="url(#amethyst-glow)"/>
    <circle cx="14" cy="12" r="1.5" fill="#CE93D8" opacity="0.6"/>
    <circle cx="42" cy="10" r="1" fill="#E1BEE7" opacity="0.5"/>
    <circle cx="48" cy="20" r="1.5" fill="#CE93D8" opacity="0.4"/>
    <circle cx="10" cy="22" r="1" fill="#BA68C8" opacity="0.5"/>
    <polygon points="28,2 44,14 44,38 28,50 12,38 12,14" fill="url(#amethyst-grad)"/>
    <polygon points="28,2 28,26 44,14" fill="rgba(255,255,255,0.25)"/>
    <polygon points="28,2 28,26 12,14" fill="rgba(0,0,0,0.1)"/>
    <polygon points="44,38 28,26 44,14" fill="rgba(255,255,255,0.1)"/>
    <polygon points="44,38 28,26 28,50" fill="rgba(0,0,0,0.2)"/>
    <polygon points="12,38 28,26 28,50" fill="rgba(255,255,255,0.05)"/>
    <polygon points="12,38 28,26 12,14" fill="rgba(0,0,0,0.15)"/>
    <polygon points="28,12 36,20 36,34 28,42 20,34 20,20" fill="none" stroke="white" stroke-width="0.5" opacity="0.3"/>
    <ellipse cx="22" cy="18" rx="6" ry="10" fill="white" opacity="0.3" transform="rotate(-10 22 18)"/>
</svg>' WHERE sort_order = 6;

-- 珍珠
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <radialGradient id="pearl-grad" cx="40%" cy="35%" r="60%">
            <stop offset="0%" style="stop-color:#FFFFFF"/>
            <stop offset="50%" style="stop-color:#F5F5F5"/>
            <stop offset="100%" style="stop-color:#E0E0E0"/>
        </radialGradient>
        <radialGradient id="pearl-shine" cx="35%" cy="30%" r="40%">
            <stop offset="0%" style="stop-color:white;stop-opacity:0.9"/>
            <stop offset="100%" style="stop-color:white;stop-opacity:0"/>
        </radialGradient>
    </defs>
    <path d="M12 34 L16 18 L22 26 L28 14 L34 26 L40 18 L44 34 Z" fill="#D4AF37" stroke="#B8860B" stroke-width="1"/>
    <rect x="12" y="34" width="32" height="6" rx="2" fill="#D4AF37" stroke="#B8860B" stroke-width="1"/>
    <circle cx="16" cy="22" r="2" fill="#E53935"/>
    <circle cx="28" cy="16" r="2.5" fill="#1E88E5"/>
    <circle cx="40" cy="22" r="2" fill="#43A047"/>
    <circle cx="28" cy="46" r="8" fill="url(#pearl-grad)" stroke="#D4C5A9" stroke-width="1"/>
    <ellipse cx="25" cy="43" rx="4" ry="3" fill="url(#pearl-shine)"/>
    <circle cx="28" cy="46" r="12" fill="white" opacity="0.15"/>
    <path d="M20 40 C24 38 32 38 36 40" fill="none" stroke="#D4AF37" stroke-width="0.8" opacity="0.5"/>
</svg>' WHERE sort_order = 7;

-- 黑曜石
UPDATE t_league_tier SET icon = '<svg viewBox="0 0 56 56" fill="none">
    <defs>
        <linearGradient id="obsidian-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#455A64"/>
            <stop offset="50%" style="stop-color:#263238"/>
            <stop offset="100%" style="stop-color:#000000"/>
        </linearGradient>
        <radialGradient id="obsidian-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" style="stop-color:#7C4DFF;stop-opacity:0.4"/>
            <stop offset="100%" style="stop-color:#7C4DFF;stop-opacity:0"/>
        </radialGradient>
        <linearGradient id="obsidian-crown" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#B0BEC5"/>
            <stop offset="50%" style="stop-color:#546E7A"/>
            <stop offset="100%" style="stop-color:#263238"/>
        </linearGradient>
    </defs>
    <circle cx="28" cy="28" r="26" fill="url(#obsidian-glow)"/>
    <circle cx="8" cy="16" r="1" fill="#B388FF" opacity="0.7"/>
    <circle cx="48" cy="12" r="1.5" fill="#7C4DFF" opacity="0.6"/>
    <circle cx="52" cy="36" r="1" fill="#B388FF" opacity="0.5"/>
    <circle cx="6" cy="40" r="1.2" fill="#7C4DFF" opacity="0.6"/>
    <path d="M10 32 L14 14 L20 22 L24 10 L28 20 L32 10 L36 22 L42 14 L46 32 Z" fill="url(#obsidian-crown)" stroke="#78909C" stroke-width="1"/>
    <rect x="10" y="32" width="36" height="7" rx="2" fill="url(#obsidian-crown)" stroke="#78909C" stroke-width="1"/>
    <circle cx="14" cy="18" r="2.5" fill="#7C4DFF">
        <animate attributeName="opacity" values="0.8;1;0.8" dur="2s" repeatCount="indefinite"/>
    </circle>
    <circle cx="28" cy="12" r="3" fill="#E040FB">
        <animate attributeName="opacity" values="0.7;1;0.7" dur="1.5s" repeatCount="indefinite"/>
    </circle>
    <circle cx="42" cy="18" r="2.5" fill="#7C4DFF">
        <animate attributeName="opacity" values="0.8;1;0.8" dur="2s" repeatCount="indefinite"/>
    </circle>
    <polygon points="28,42 34,46 32,52 24,52 22,46" fill="url(#obsidian-grad)" stroke="#546E7A" stroke-width="1"/>
    <polygon points="28,42 28,48 34,46" fill="rgba(255,255,255,0.2)"/>
    <polygon points="28,42 28,48 22,46" fill="rgba(0,0,0,0.3)"/>
    <ellipse cx="28" cy="48" rx="3" ry="2" fill="#7C4DFF" opacity="0.8">
        <animate attributeName="rx" values="3;4;3" dur="3s" repeatCount="indefinite"/>
    </ellipse>
</svg>' WHERE sort_order = 8;
