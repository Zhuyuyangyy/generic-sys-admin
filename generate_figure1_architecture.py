from __future__ import annotations

from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
from matplotlib.patches import Circle, FancyArrowPatch, Polygon, Rectangle


WIDTH_PX = 1800
HEIGHT_PX = 1000
FIGURE_DPI = 100
NL_FILL = "#D9E1F2"
CAUSAL_FILL = "#E2EFDA"
ARROW_COLOR = "#4D4D4D"
EDGE_COLOR = "#404040"
TEXT_COLOR = "#202020"

ARIAL_PATH = Path(r"C:\Windows\Fonts\arial.ttf")
FONT = FontProperties(fname=str(ARIAL_PATH)) if ARIAL_PATH.exists() else None


def _text(ax, x, y, text, size=18, weight="normal", ha="center", va="center", **kwargs):
    params = {
        "x": x,
        "y": y,
        "s": text,
        "fontsize": size,
        "fontweight": weight,
        "ha": ha,
        "va": va,
        "color": TEXT_COLOR,
    }
    if FONT is not None:
        params["fontproperties"] = FONT
    params.update(kwargs)
    return ax.text(**params)


def _draw_box(ax, x, y, w, h, label, fill, size=20):
    rect = Rectangle((x, y), w, h, linewidth=2.2, edgecolor=EDGE_COLOR, facecolor=fill)
    ax.add_patch(rect)
    _text(ax, x + w / 2, y + h / 2, label, size=size, weight="bold")
    return rect


def _draw_arrow(ax, start, end, label=None, dashed=False, curve=0.0, label_offset=(0, 0), linewidth=2.2):
    arrow = FancyArrowPatch(
        start,
        end,
        arrowstyle="-|>",
        mutation_scale=18,
        linewidth=linewidth,
        linestyle="--" if dashed else "-",
        color=ARROW_COLOR,
        connectionstyle=f"arc3,rad={curve}",
        shrinkA=2,
        shrinkB=2,
    )
    ax.add_patch(arrow)
    if label:
        label_x = (start[0] + end[0]) / 2 + label_offset[0]
        label_y = (start[1] + end[1]) / 2 + label_offset[1]
        _text(
            ax,
            label_x,
            label_y,
            label,
            size=15,
            bbox={"facecolor": "white", "edgecolor": "none", "pad": 1.5},
        )
    return arrow


def _draw_user_icon(ax, cx, cy):
    head = Circle((cx, cy + 42), 22, linewidth=2, edgecolor=EDGE_COLOR, facecolor="white")
    body = Circle((cx, cy - 2), 34, linewidth=2, edgecolor=EDGE_COLOR, facecolor="white")
    ax.add_patch(head)
    ax.add_patch(body)
    ax.add_patch(Rectangle((cx - 34, cy - 30), 68, 20, linewidth=0, facecolor="white"))


def _draw_warning_icon(ax, x, y):
    triangle = Polygon(
        [(x, y + 18), (x - 16, y - 12), (x + 16, y - 12)],
        closed=True,
        edgecolor=EDGE_COLOR,
        facecolor="#FCE4D6",
        linewidth=1.6,
    )
    ax.add_patch(triangle)
    _text(ax, x, y - 1, "!", size=16, weight="bold")


def render_architecture(output_path: Path | str = "figure1_architecture.png") -> Path:
    output_path = Path(output_path)

    fig = plt.figure(figsize=(WIDTH_PX / FIGURE_DPI, HEIGHT_PX / FIGURE_DPI), dpi=FIGURE_DPI)
    fig.patch.set_facecolor("white")
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(0, WIDTH_PX)
    ax.set_ylim(0, HEIGHT_PX)
    ax.axis("off")

    _text(
        ax,
        WIDTH_PX / 2,
        955,
        "Figure 1. Overall architecture of the\nproposed NL-driven causal-aware ERM system",
        size=23,
        weight="bold",
    )

    _text(ax, 290, 810, "Natural Language Operation Layer", size=21, weight="bold", ha="left")
    _text(ax, 290, 470, "Causal-Aware Layer", size=21, weight="bold", ha="left")
    ax.plot([280, 1560], [780, 780], color="#CFCFCF", linewidth=1.5)
    ax.plot([280, 1560], [440, 440], color="#CFCFCF", linewidth=1.5)

    upper_y = 620
    lower_y = 230
    box_w = 220
    box_h = 100

    parser = _draw_box(ax, 370, upper_y, box_w, box_h, "NLParser", NL_FILL)
    rule_engine = _draw_box(ax, 675, upper_y, box_w, box_h, "NLRuleEngine", NL_FILL)
    executor = _draw_box(ax, 980, upper_y, box_w, box_h, "NLExecutor", NL_FILL)

    dag_service = _draw_box(ax, 600, lower_y, 280, box_h, "CausalDAGService", CAUSAL_FILL, size=19)
    propagation = _draw_box(
        ax,
        980,
        lower_y,
        330,
        box_h,
        "CausalPropagationEngine",
        CAUSAL_FILL,
        size=19,
    )

    _draw_user_icon(ax, 120, upper_y + 28)
    _text(ax, 170, upper_y + 28, "Natural Language\nCommand", size=18, weight="bold", ha="left")

    exec_result = _draw_box(ax, 1465, upper_y + 10, 220, 86, "ExecutionResult", "white", size=18)
    causal_result = _draw_box(ax, 1465, lower_y + 7, 250, 100, "CausalCheckResult", "white", size=18)
    _draw_warning_icon(ax, 1510, lower_y + 28)
    _text(ax, 1540, lower_y + 25, "hasHighImpact", size=14, ha="left")

    _draw_arrow(ax, (310, upper_y + 50), (370, upper_y + 50))
    _draw_arrow(
        ax,
        (370 + box_w, upper_y + 50),
        (675, upper_y + 50),
        label="ParseResult",
        label_offset=(0, 38),
    )
    _draw_arrow(ax, (675 + box_w, upper_y + 50), (980, upper_y + 50))
    _draw_arrow(ax, (980 + box_w, upper_y + 50), (1465, upper_y + 53))

    _draw_arrow(
        ax,
        (600 + 280, lower_y + 50),
        (980, lower_y + 50),
        label="propagate()",
        label_offset=(0, 36),
    )
    _draw_arrow(ax, (980 + 330, lower_y + 50), (1465, lower_y + 57))

    _draw_arrow(
        ax,
        (1090, upper_y),
        (740, lower_y + box_h),
        label="if UPDATE or DELETE",
        dashed=True,
        label_offset=(-40, -10),
    )
    _draw_arrow(
        ax,
        (1160, lower_y + box_h),
        (1110, upper_y),
        label="CausalCheckResult",
        curve=0.28,
        label_offset=(120, 48),
    )

    fig.savefig(output_path, dpi=FIGURE_DPI, facecolor="white")
    plt.close(fig)
    return output_path


def main():
    render_architecture()


if __name__ == "__main__":
    main()
