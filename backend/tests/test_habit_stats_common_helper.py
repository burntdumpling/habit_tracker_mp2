from __future__ import annotations

from datetime import date

import pytest

from app.services.helpers.habit_stats_common import (
    build_month_points,
    build_week_points,
    build_year_points,
    compute_completion_rate,
)


@pytest.fixture(autouse=True)
def clean_db() -> None:
    return None


def test_compute_completion_rate_returns_zero_when_expected_is_zero() -> None:
    assert compute_completion_rate(expected_count=0, completed_count=5) == 0


def test_compute_completion_rate_clamps_to_hundred() -> None:
    assert compute_completion_rate(expected_count=2, completed_count=5) == 100


def test_build_week_points_returns_seven_points_with_stable_labels_and_future_flags() -> None:
    points = build_week_points(
        range_start=date(2026, 3, 16),
        range_end=date(2026, 3, 22),
        today=date(2026, 3, 18),
        expected_dates={date(2026, 3, 16), date(2026, 3, 20)},
        completed_dates={date(2026, 3, 16)},
    )

    assert len(points) == 7
    assert [point.label for point in points] == ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    assert [point.is_future for point in points] == [False, False, False, True, True, True, True]
    assert points[0].key == "2026-03-16"
    assert points[0].value == 100.0
    assert points[4].value == 0.0


def test_build_month_points_returns_one_point_per_day_for_31_day_month() -> None:
    points = build_month_points(
        range_start=date(2026, 3, 1),
        range_end=date(2026, 3, 31),
        today=date(2026, 3, 15),
        expected_dates={date(2026, 3, 1), date(2026, 3, 31)},
        completed_dates={date(2026, 3, 1)},
    )

    assert len(points) == 31
    assert points[0].label == "1"
    assert points[-1].label == "31"
    assert points[-1].is_future is True


def test_build_year_points_returns_twelve_points_with_stable_month_labels() -> None:
    points = build_year_points(
        range_start=date(2026, 1, 1),
        range_end=date(2026, 12, 31),
        today=date(2026, 4, 15),
        expected_dates=set(),
        completed_dates=set(),
    )

    assert len(points) == 12
    assert [point.label for point in points] == [
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
    ]


def test_build_year_points_uses_monthly_completed_expected_ratio_for_value() -> None:
    points = build_year_points(
        range_start=date(2026, 1, 1),
        range_end=date(2026, 12, 31),
        today=date(2026, 12, 31),
        expected_dates={date(2026, 1, 2), date(2026, 1, 10), date(2026, 1, 18)},
        completed_dates={date(2026, 1, 2)},
    )

    january_point = points[0]
    february_point = points[1]

    assert january_point.completed == 1
    assert january_point.expected == 3
    assert january_point.value == 33.0
    assert february_point.completed == 0
    assert february_point.expected == 0
    assert february_point.value == 0.0
