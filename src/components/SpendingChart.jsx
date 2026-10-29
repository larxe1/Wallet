import React from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import './SpendingChart.css';

const COLORS = ['#1fc77a', '#34d399', '#ef5b6b', '#e6b04b', '#5eb8a0', '#4fa3c7', '#c08ad6', '#a3c94a'];

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
              tick={{ fill: '#9bb8a9', fontSize: 12, fontFamily: 'JetBrains Mono' }}
            />
            <YAxis 
              type="category" 
              dataKey="categoryName" 
              axisLine={false}
              tickLine={false}
              tick={{ fill: '#eef6f1', fontSize: 14, fontFamily: 'Inter' }}
              width={100}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(120, 190, 150, 0.1)' }} />
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
