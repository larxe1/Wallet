import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import './CategoryPieChart.css';

const COLORS = ['#6366f1', '#10b981', '#f43f5e', '#f59e0b', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16'];

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const CustomTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    const total = payload[0].payload.totalSum;
    const percent = ((data.value / total) * 100).toFixed(1);
    
    return (
      <div className="pie-tooltip">
        <p className="pie-tooltip-title">
          <span className="pie-tooltip-color" style={{ backgroundColor: payload[0].color }}></span>
          {data.icon} {data.name}
        </p>
        <p className="pie-tooltip-value">{formatCurrency(data.value)}</p>
        <p className="pie-tooltip-percent">{percent}%</p>
      </div>
    );
  }
  return null;
};

const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
  const x = cx + radius * Math.cos(-midAngle * (Math.PI / 180));
  const y = cy + radius * Math.sin(-midAngle * (Math.PI / 180));

  if (percent < 0.05) return null; // Don't show label for very small slices

  return (
    <text 
      x={x} 
      y={y} 
      fill="#fff" 
      textAnchor="middle" 
      dominantBaseline="central"
      className="pie-label"
    >
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

export default function CategoryPieChart({ data = [], title = 'Category Breakdown' }) {
  const totalAmount = data.reduce((sum, item) => sum + item.value, 0);
  
  // Inject totalSum into data for the tooltip
  const chartData = data.map(item => ({ ...item, totalSum: totalAmount }));

  return (
    <div className="pie-chart-container">
      <h3 className="pie-chart-title">{title}</h3>
      <div className="pie-chart-wrapper">
        <div className="pie-chart-center-text">
          <span className="pie-center-label">Total</span>
          <span className="pie-center-value">{formatCurrency(totalAmount)}</span>
        </div>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={80}
              outerRadius={120}
              paddingAngle={5}
              dataKey="value"
              labelLine={false}
              label={renderCustomizedLabel}
              animationDuration={1500}
              stroke="none"
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
            <Legend 
              verticalAlign="bottom" 
              height={36}
              iconType="circle"
              wrapperStyle={{ fontSize: '12px', fontFamily: 'Inter', color: '#94a3b8' }}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
