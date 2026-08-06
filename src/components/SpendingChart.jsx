import React from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import './SpendingChart.css';

const COLORS = ['#6366f1', '#10b981', '#f43f5e', '#f59e0b', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16'];

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const CustomTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="chart-tooltip">
        <p className="tooltip-title">{data.categoryIcon} {data.categoryName}</p>
        <p className="tooltip-value">{formatCurrency(data.totalAmount)}</p>
        {data.walletName && <p className="tooltip-wallet">Wallet: {data.walletName}</p>}
      </div>
    );
  }
  return null;
};

export default function SpendingChart({ data = [], title = 'Spending by Category' }) {
  return (
    <div className="spending-chart-container">
      <h3 className="chart-title">{title}</h3>
      <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={data}
            layout="vertical"
            margin={{ top: 10, right: 30, left: 40, bottom: 0 }}
          >
            <XAxis 
              type="number" 
              tickFormatter={(val) => `₱${val > 1000 ? (val/1000).toFixed(0) + 'k' : val}`}
              axisLine={false}
              tickLine={false}
              tick={{ fill: '#94a3b8', fontSize: 12, fontFamily: 'JetBrains Mono' }}
            />
            <YAxis 
              type="category" 
              dataKey="categoryName" 
              axisLine={false}
              tickLine={false}
              tick={{ fill: '#f1f5f9', fontSize: 14, fontFamily: 'Inter' }}
              width={100}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(148, 163, 184, 0.1)' }} />
            <Bar 
              dataKey="totalAmount" 
              radius={[0, 4, 4, 0]}
              animationDuration={1500}
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
